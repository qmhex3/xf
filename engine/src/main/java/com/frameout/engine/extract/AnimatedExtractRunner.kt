package com.frameout.engine.extract

import com.frameout.core.util.SmoothScale
import android.graphics.Bitmap
import android.net.Uri
import com.frameout.core.db.FrameDao
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectDao
import com.frameout.core.model.ProjectStatus
import com.frameout.core.storage.StorageManager
import com.frameout.engine.decode.AnimatedFrameSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class AnimatedExtractRunner(
    private val newDecoder: () -> AnimatedFrameSource,
    private val projectDao: ProjectDao,
    private val frameDao: FrameDao,
    private val storage: StorageManager,
    private val checkpointStore: CheckpointStore,
    private val isCancelled: () -> Boolean,
    private val isPaused: () -> Boolean,
    private val onProgress: (Int, Int, Long) -> Unit
) {
    private var gate: com.frameout.core.quality.ExtractQualityGate? = null
    private val flushClock = FlushClock()
    private var skipped = 0
    private var consecutiveFailures = 0
    private var sinceSpaceCheck = 0

    suspend fun run(request: ExtractRequest, uri: Uri) {
        com.frameout.core.util.RuntimeGate.pass()
        val decoder = newDecoder()
        try {
            decoder.open(uri)
            val project = projectDao.getById(request.projectId)
            val state = createState(request, decoder, project)
            extract(request, decoder, state)
        } finally { decoder.close() }
    }

    private suspend fun createState(request: ExtractRequest, decoder: AnimatedFrameSource, project: com.frameout.core.db.ProjectEntity?): LoopState {
        val done = frameDao.countAll(request.projectId)
        gate = GateResume.gate(request, frameDao, done > 0)
        val lastPts = if (done > 0) project?.lastPtsUs ?: -1L else -1L
        val lastIndex = if (done > 0) project?.lastSampleIndex ?: -1 else -1
        val selectedCount = AnimatedSelection.count(decoder, request.mode, request.startMs, request.endMs, lastPts, lastIndex)
        projectDao.updateIndexMeta(
            request.projectId,
            decoder.durationUs() / 1000L,
            decoder.frameCount(),
            FfmpegSelection.encodeMode(request.mode),
            request.autoSave,
            request.saveFormat,
            request.jpegQuality,
            request.startMs,
            request.endMs,
            System.currentTimeMillis()
        )
        if (!storage.hasSpaceForResolution(SpaceEstimate.frames(request.mode, selectedCount), decoder.videoWidth(), decoder.videoHeight(), request.autoSave, request.saveFormat)) {
            throw LowSpaceException()
        }
        val startStatus = if (isPaused()) ProjectStatus.Paused else ProjectStatus.Extracting
        projectDao.updateProgress(
            request.projectId,
            startStatus.name,
            done,
            lastPts.coerceAtLeast(0L),
            System.currentTimeMillis()
        )
        projectDao.setTargetCount(request.projectId, done + selectedCount)
        return LoopState(done, lastPts, if (done > 0) ResumeReference.load(frameDao, request.projectId, request.mode) else null, project?.thumbnailPath != null, done + selectedCount, lastIndex)
    }

    private suspend fun extract(request: ExtractRequest, decoder: AnimatedFrameSource, state: LoopState) {
        val batch = mutableListOf<FrameEntity>()
        try {
            collectFrames(request, decoder, state, batch)
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                val status = if (isCancelled()) ProjectStatus.Cancelled else ProjectStatus.Paused
                checkpointStore.commit(request.projectId, status, state.done, state.lastPts, state.lastIndex, batch.toList())
            }
            throw e
        } catch (e: Throwable) {
            withContext(NonCancellable) {
                runCatching { checkpointStore.commit(request.projectId, ProjectStatus.Extracting, state.done, state.lastPts, state.lastIndex, batch.toList()) }
            }
            throw e
        }
        val finalStatus = when {
            isCancelled() -> ProjectStatus.Cancelled
            isPaused() -> ProjectStatus.Paused
            else -> ProjectStatus.Completed
        }
        checkpointStore.commit(request.projectId, finalStatus, state.done, state.lastPts, state.lastIndex, batch.toList())
        if (finalStatus == ProjectStatus.Completed) projectDao.setTargetCount(request.projectId, state.done)
    }

    private suspend fun collectFrames(
        request: ExtractRequest,
        decoder: AnimatedFrameSource,
        state: LoopState,
        batch: MutableList<FrameEntity>
    ) {
        val indices = AnimatedSelection.flow(decoder, request.mode, request.startMs, request.endMs, state.lastPts, state.lastIndex)
        val tally = ProgressTally(state.progressTotal - state.done, state.done)
        indices.collect { index ->
            ensureRunning()
            tally.advance()
            val frame = processFrame(request, decoder, index, state.previousHistogram, state.progressTotal)
            state.lastIndex = index
            if (frame != null) {
                state.previousHistogram = frame.histogram
                state.lastPts = frame.pts
                frame.entity?.let { accept(request, state, batch, it) }
                if (batch.size >= 50 || (batch.isNotEmpty() && flushClock.due()) || (spaceCheckDue() && !storage.hasHeadroom())) flush(request.projectId, state.done, state.lastPts, state.lastIndex, batch)
            }
            tally.persist(projectDao, request.projectId, state.done)
            onProgress(state.done, tally.total(state.done), state.lastPts)
        }
    }

    private suspend fun accept(request: ExtractRequest, state: LoopState, batch: MutableList<FrameEntity>, entity: FrameEntity) {
        batch += entity
        state.done++
        if (!state.thumbnailSet) {
            projectDao.setThumbnail(request.projectId, entity.thumbnailPath, System.currentTimeMillis())
            state.thumbnailSet = true
        }
    }

    private fun spaceCheckDue(): Boolean {
        if (++sinceSpaceCheck < SPACE_CHECK_EVERY) return false
        sinceSpaceCheck = 0
        return true
    }

    private fun decodeOrSkip(decoder: AnimatedFrameSource, index: Int, total: Int): Pair<Long, Bitmap>? {
        val decoded = try {
            decoder.decodeFrame(index)
        } catch (e: Exception) {
            com.frameout.core.util.DiagLog.append("extract", "animated frame $index failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
        if (decoded != null) return decoded
        skipped++
        consecutiveFailures++
        com.frameout.core.util.DiagLog.append("extract", "skipped animated frame $index")
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES || skipped > maxOf(MIN_SKIP_BUDGET, total / SKIP_BUDGET_DIVISOR)) {
            error("Animated frame decode failed at index $index")
        }
        return null
    }

    private companion object {
        const val SPACE_CHECK_EVERY = 8
        const val MAX_CONSECUTIVE_FAILURES = 25
        const val MIN_SKIP_BUDGET = 100
        const val SKIP_BUDGET_DIVISOR = 5
    }

    private suspend fun updateStatus(projectId: Long, state: LoopState, cancelled: Boolean) {
        val status = when {
            cancelled -> ProjectStatus.Cancelled
            isPaused() -> ProjectStatus.Paused
            else -> ProjectStatus.Completed
        }
        projectDao.updateProgress(projectId, status.name, state.done, state.lastPts.coerceAtLeast(0L), System.currentTimeMillis())
    }

    private suspend fun ensureRunning() = awaitRunnable(isCancelled, isPaused)

    private data class Processed(val entity: FrameEntity?, val pts: Long, val histogram: FloatArray?)
    private data class LoopState(
        var done: Int,
        var lastPts: Long,
        var previousHistogram: FloatArray?,
        var thumbnailSet: Boolean,
        val progressTotal: Int,
        var lastIndex: Int
    )

    private suspend fun processFrame(
        request: ExtractRequest,
        decoder: AnimatedFrameSource,
        index: Int,
        previousHistogram: FloatArray?,
        total: Int
    ): Processed? {
        val decoded = decodeOrSkip(decoder, index, total) ?: return null
        consecutiveFailures = 0
        val pts = decoded.first
        val bitmap = decoded.second
        val histogram = SceneChangeSelector.signature(request.mode, bitmap) ?: previousHistogram
        val keep = when (request.mode) {
            ExtractMode.KeyframesOnly -> true
            ExtractMode.SceneChange, ExtractMode.Slides -> SceneChangeSelector.changed(request.mode, previousHistogram, histogram)
            else -> true
        }
        if (!keep || gate?.acceptAsync(bitmap) == false) {
            bitmap.recycle()
            return Processed(null, pts, SceneChangeSelector.carry(request.mode, previousHistogram, histogram))
        }
        try {
            val thumb = scaleThumb(bitmap)
            val thumbPath = try {
                storage.writeThumb(request.projectId, index, thumb)
            } finally {
                if (thumb !== bitmap) thumb.recycle()
            }
            val fullPath = if (request.autoSave) {
                storage.writeFrame(request.projectId, index, bitmap, request.saveFormat, request.jpegQuality)
            } else {
                null
            }
            AnalysisCopy.write(storage, request, index, bitmap)
            val entity = FrameEntity(projectId = request.projectId, index = index, ptsUs = pts, thumbnailPath = thumbPath, fullPath = fullPath)
            return Processed(entity, pts, histogram)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private suspend fun flush(projectId: Long, done: Int, pts: Long, sampleIndex: Int, batch: MutableList<FrameEntity>) {
        checkpointStore.commit(projectId, ProjectStatus.Extracting, done, pts, sampleIndex, batch.toList())
        batch.clear()
        if (!storage.hasHeadroom()) throw LowSpaceException()
    }

    private fun scaleThumb(src: Bitmap): Bitmap {
        val max = com.frameout.core.storage.ThumbSpec.MAX_SIDE
        if (src.width <= max && src.height <= max) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, false)
        val factor = max.toFloat() / maxOf(src.width, src.height)
        return SmoothScale.scaled(src, (src.width * factor).toInt(), (src.height * factor).toInt(), true)
    }
}
