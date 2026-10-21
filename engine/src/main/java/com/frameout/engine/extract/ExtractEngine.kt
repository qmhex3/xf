package com.frameout.engine.extract
import android.content.Context
import android.net.Uri
import com.frameout.codecpack.CodecPack
import com.frameout.codecpack.FormatInfo
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FormatProbe
import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureException
import com.frameout.core.util.FailureMarker
import com.frameout.core.db.FrameDao
import com.frameout.core.db.ProjectDao
import com.frameout.core.db.ProjectEntity
import com.frameout.core.db.VideoIndexDao
import com.frameout.core.db.progressTotal
import com.frameout.core.media.MotionPhotoExtractor
import com.frameout.core.model.ProjectStatus
import com.frameout.core.storage.StorageManager
import com.frameout.engine.index.IndexReuse
import com.frameout.engine.work.ExtractControl
import com.frameout.core.util.DiagLog
import com.frameout.engine.decode.AnimatedFrameSource
import com.frameout.engine.decode.AnimatedSources
import com.frameout.engine.decode.ApngFrameDecoder
import com.frameout.engine.decode.GifFrameDecoder
import com.frameout.engine.decode.WebpFrameDecoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
class ExtractEngine(
    private val context: Context,
    private val projectDao: ProjectDao,
    private val frameDao: FrameDao,
    private val storage: StorageManager,
    private val videoIndexDao: VideoIndexDao,
    private val checkpointStore: CheckpointStore
) {
    private val _progress = MutableStateFlow(ExtractProgress(0, ExtractProgress.Status.Idle, 0, 0, 0, 0))
    val progress: StateFlow<ExtractProgress> = _progress.asStateFlow()

    @Volatile private var cancelled = false
    @Volatile private var paused = false

    fun cancel() { cancelled = true }
    fun pause() { paused = true }
    fun resume() { paused = false }

    suspend fun run(request: ExtractRequest) = withContext(Dispatchers.IO) {
        cancelled = ExtractControl.isCancelled(request.projectId)
        paused = ExtractControl.isPaused(request.projectId)
        val startTime = System.currentTimeMillis()
        try {
            FailureMarker.clear(context, request.projectId)
            sweepOrphanFiles(request.projectId)
            emit(request.projectId, ExtractProgress.Status.Indexing, 0, 0, startTime, 0)
            dispatch(request, startTime)
        } catch (e: CancellationException) {
            handleCancellation(request, startTime)
            throw e
        } catch (e: Exception) {
            ExtractFailureHandler.handle(projectDao, request, startTime, e, cancelled, ::emit)
        } catch (e: OutOfMemoryError) {
            ExtractFailureHandler.handle(projectDao, request, startTime, RuntimeException("Out of memory", e), cancelled, ::emit)
        }
    }

    private suspend fun sweepOrphanFiles(projectId: Long) {
        try {
            val known = frameDao.idsByIndex(projectId).mapTo(HashSet()) { it.frameIndex }
            val removed = storage.sweepOrphanFrameFiles(projectId, known)
            if (removed > 0) DiagLog.append("extract", "removed $removed orphan frame files for project $projectId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DiagLog.append("extract", "orphan sweep failed: ${e.message}")
        }
    }

    private suspend fun dispatch(request: ExtractRequest, startTime: Long) {
        val uri = Uri.parse(request.uri)
        val info = FormatProbe.probe(context, uri)
        val target = resolveUri(uri, info)
        when (info.kind) {
            FormatKind.Gif -> runAnimated(request, uri, startTime) { GifFrameDecoder(context) }
            FormatKind.Apng -> runAnimated(request, uri, startTime) { ApngFrameDecoder(context) }
            FormatKind.AnimatedWebp -> runAnimated(request, uri, startTime) { WebpFrameDecoder(context) }
            FormatKind.NeedsFfmpeg -> runFfmpeg(request, uri, startTime, info.message)
            FormatKind.Unsupported -> runUnsupported(request, uri, target, startTime, info)
            else -> runSystemOrFfmpeg(request, uri, target, startTime)
        }
    }

    private suspend fun runUnsupported(request: ExtractRequest, uri: Uri, target: Uri, startTime: Long, info: FormatInfo) {
        if (target != uri) {
            runSystem(request, target, startTime)
            return
        }
        val reason = info.failure.takeIf { it != MediaFailure.None } ?: MediaFailure.UnsupportedContainer
        projectDao.setStatus(request.projectId, ProjectStatus.Failed.name, System.currentTimeMillis())
        DiagLog.append("extract", "${reason.name}: ${info.message}")
        emit(request.projectId, ExtractProgress.Status.Failed, 0, 0, startTime, 0, ExtractProgress.failureMessage(reason))
    }

    private suspend fun handleCancellation(request: ExtractRequest, startTime: Long) {
        withContext(NonCancellable) {
            val saved = projectDao.getById(request.projectId)
            val cancelledNow = cancelled || saved?.status == ProjectStatus.Cancelled.name
            val pausedByUser = paused || ExtractControl.isPaused(request.projectId) || saved?.status == ProjectStatus.Paused.name
            val status = when {
                cancelledNow -> ProjectStatus.Cancelled
                pausedByUser -> ProjectStatus.Paused
                else -> ProjectStatus.Queued
            }
            val progressStatus = if (cancelledNow) ExtractProgress.Status.Cancelled else ExtractProgress.Status.Paused
            projectDao.setStatus(request.projectId, status.name, System.currentTimeMillis())
            val message = if (cancelledNow) "Cancelled" else ExtractProgress.MESSAGE_PAUSED_SYSTEM
            emitSaved(request.projectId, progressStatus, saved, startTime, message)
        }
    }

    private fun resolveUri(uri: Uri, info: FormatInfo): Uri {
        val direct = AnimatedSources.handles(info.kind) || info.kind == FormatKind.NeedsFfmpeg
        return if (direct) uri else MotionPhotoExtractor.resolveVideoSource(context, uri)
    }

    private suspend fun runAnimated(
        request: ExtractRequest,
        uri: Uri,
        startTime: Long,
        newDecoder: () -> AnimatedFrameSource
    ) {
        projectDao.setStatus(request.projectId, ProjectStatus.Extracting.name, System.currentTimeMillis())
        val runner = AnimatedExtractRunner(
            newDecoder, projectDao, frameDao, storage, checkpointStore,
            { cancelled }, { paused }, extractingCallback(request, startTime)
        )
        try {
            runner.run(request, uri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            throw MediaFailureException(MediaFailure.CorruptData, e.message, e)
        }
        emitFinal(request.projectId, startTime)
    }

    private suspend fun runSystemOrFfmpeg(request: ExtractRequest, uri: Uri, target: Uri, startTime: Long) {
        try {
            runSystem(request, target, startTime)
        } catch (e: CancellationException) {
            throw e
        } catch (e: LowSpaceException) {
            throw e
        } catch (e: Exception) {
            if (e.isDiskFull()) throw LowSpaceException()
            val untouched = frameDao.countAll(request.projectId) == 0
            val recoverable = e is FrameDecodeException
            if ((!untouched && !recoverable) || !CodecPack.isInstalled(context)) throw e
            DiagLog.append("extract", "system decoder failed, switching to codec pack: $e${e.cause?.let { " caused by $it" }.orEmpty()}")
            videoIndexDao.clearIndex(request.projectId)
            IndexReuse.clearMarker(storage.projectDir(request.projectId))
            runFfmpeg(request, uri, startTime, e.message.orEmpty(), resumeByPts = !untouched)
        }
    }

    private suspend fun runSystem(request: ExtractRequest, uri: Uri, startTime: Long) {
        val extraction = SystemExtraction(
            context, projectDao, frameDao, storage, videoIndexDao, checkpointStore,
            { cancelled }, { paused },
            extractingCallback(request, startTime),
            { done, total -> emit(request.projectId, ExtractProgress.Status.Indexing, done, total, startTime, 0L) }
        )
        extraction.run(request, uri)
        val project = projectDao.getById(request.projectId)
        emitSaved(request.projectId, systemStatus(project?.status), project, startTime)
    }

    private fun systemStatus(stored: String?): ExtractProgress.Status = when (stored) {
        ProjectStatus.Failed.name -> ExtractProgress.Status.Failed
        ProjectStatus.Paused.name -> ExtractProgress.Status.Paused
        else -> if (cancelled) ExtractProgress.Status.Cancelled else ExtractProgress.Status.Completed
    }

    private suspend fun runFfmpeg(request: ExtractRequest, uri: Uri, startTime: Long, failMsg: String, resumeByPts: Boolean = false) {
        try {
            val runner = FfmpegExtractRunner(
                context, projectDao, frameDao, storage, videoIndexDao, checkpointStore,
                { cancelled }, { paused }, extractingCallback(request, startTime)
            )
            runner.run(request, uri, resumeByPts)
            emitFinal(request.projectId, startTime)
        } catch (e: CancellationException) {
            throw e
        } catch (e: LowSpaceException) {
            throw e
        } catch (e: Exception) {
            if (e.isDiskFull()) throw LowSpaceException()
            failProject(request.projectId, startTime, ExtractFailureHandler.wireMessage(e, false), e.message ?: failMsg)
        } catch (e: OutOfMemoryError) {
            failProject(request.projectId, startTime, ExtractProgress.failureMessage(MediaFailure.OutOfMemory), "Out of memory")
        }
    }

    private fun extractingCallback(request: ExtractRequest, startTime: Long): (Int, Int, Long) -> Unit =
        { done, total, pts -> emit(request.projectId, ExtractProgress.Status.Extracting, done, total, startTime, pts) }

    private suspend fun emitFinal(projectId: Long, startTime: Long) {
        val status = when {
            cancelled -> ExtractProgress.Status.Cancelled
            paused -> ExtractProgress.Status.Paused
            else -> ExtractProgress.Status.Completed
        }
        emitSaved(projectId, status, projectDao.getById(projectId), startTime)
    }

    private suspend fun failProject(projectId: Long, startTime: Long, message: String, detail: String = message) {
        val project = projectDao.getById(projectId)
        withContext(NonCancellable) {
            projectDao.updateProgress(
                projectId,
                ProjectStatus.Failed.name,
                project?.extractedCount ?: 0,
                project?.lastPtsUs ?: 0L,
                System.currentTimeMillis()
            )
        }
        emitSaved(projectId, ExtractProgress.Status.Failed, project, startTime, message)
        DiagLog.append("extract", detail)
    }

    private fun emitSaved(
        projectId: Long,
        status: ExtractProgress.Status,
        project: ProjectEntity?,
        startTime: Long,
        message: String = ""
    ) {
        val done = project?.extractedCount ?: 0
        val total = if (status == ExtractProgress.Status.Completed) done else project?.progressTotal ?: 0
        emit(projectId, status, done, total, startTime, project?.lastPtsUs ?: 0L, message)
    }

    private fun emit(
        projectId: Long,
        status: ExtractProgress.Status,
        done: Int,
        total: Int,
        startTime: Long,
        pts: Long,
        message: String = ""
    ) {
        val elapsed = System.currentTimeMillis() - startTime
        recordFailureMarker(projectId, status, message)
        _progress.value = ExtractProgress(projectId, status, done, total, elapsed, pts, message)
    }

    private fun recordFailureMarker(projectId: Long, status: ExtractProgress.Status, message: String) {
        if (status != ExtractProgress.Status.Failed) return
        ExtractProgress.failureOf(message)?.let { FailureMarker.write(context, projectId, it) }
    }
}
