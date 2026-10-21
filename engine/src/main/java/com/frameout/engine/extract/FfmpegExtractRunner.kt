package com.frameout.engine.extract

import android.content.Context
import android.net.Uri
import com.frameout.codecpack.CodecPack
import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureException
import com.frameout.core.db.FrameDao
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectDao
import com.frameout.core.db.VideoIndexDao
import com.frameout.core.model.ProjectStatus
import com.frameout.core.storage.StorageManager
import com.frameout.engine.decode.DecodeRouter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class FfmpegExtractRunner(
    private val context: Context,
    private val projectDao: ProjectDao,
    private val frameDao: FrameDao,
    private val storage: StorageManager,
    private val videoIndexDao: VideoIndexDao,
    private val checkpointStore: CheckpointStore,
    private val isCancelled: () -> Boolean,
    private val isPaused: () -> Boolean,
    private val onProgress: (Int, Int, Long) -> Unit
) {
    private val flushClock = FlushClock()
    private var sinceSpaceCheck = 0

    suspend fun run(request: ExtractRequest, uri: Uri, resumeByPts: Boolean = false) {
        if (!CodecPack.load(context)) {
            throw MediaFailureException(MediaFailure.CodecPackMissing, "${CodecPack.status(context)}; import the verified codec pack in Settings")
        }
        val router = DecodeRouter(context) { isCancelled() }
        router.reuseCopy = true
        router.forceFfmpeg = true
        var finished = false
        try {
            val planner = FfmpegSessionPlanner(context, projectDao, frameDao, storage, videoIndexDao) { isCancelled() }
            val session = planner.plan(request, uri, router, resumeByPts)
            val processor = FfmpegFrameProcessor(storage, videoIndexDao, GateResume.gate(request, frameDao, session.done > 0))
            supervise(request, session, processor, router)
            finished = true
        } finally {
            router.retainCopy = !finished && !isCancelled()
            router.close()
        }
    }

    private suspend fun supervise(request: ExtractRequest, session: FfmpegSession, processor: FfmpegFrameProcessor, router: DecodeRouter) = coroutineScope {
        val watcher = launch(Dispatchers.IO) {
            while (isActive && !isCancelled()) delay(CANCEL_POLL_MS)
            if (isActive) router.cancelNative()
        }
        val ended = AtomicBoolean(false)
        val hook = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                if (!ended.get()) router.cancelNative()
            }
        }
        val indexer = launch(Dispatchers.IO) { session.index.finish() }
        try {
            extract(request, session, processor)
        } finally {
            ended.set(true)
            hook.cancel()
            watcher.cancel()
            indexer.cancel()
        }
    }

    private suspend fun extract(request: ExtractRequest, session: FfmpegSession, processor: FfmpegFrameProcessor) {
        val batch = mutableListOf<FrameEntity>()
        markStarted(request, session)
        try {
            collectFrames(request, session, processor, batch, ProgressTally(session.progressTotal - session.done, session.done))
        } catch (_: CancellationException) {
            withContext(NonCancellable) { commit(request, session, batch, interruptedStatus()) }
            throw CancellationException()
        } catch (e: Throwable) {
            withContext(NonCancellable) { runCatching { commit(request, session, batch, ProjectStatus.Extracting) } }
            throw e
        }
        withContext(NonCancellable) {
            val status = finalStatus()
            commit(request, session, batch, status)
            if (status == ProjectStatus.Completed) projectDao.setTargetCount(request.projectId, session.done)
        }
    }

    private suspend fun markStarted(request: ExtractRequest, session: FfmpegSession) {
        projectDao.setTargetCount(request.projectId, session.progressTotal)
        val status = if (isPaused()) ProjectStatus.Paused else ProjectStatus.Extracting
        projectDao.updateProgress(
            request.projectId,
            status.name,
            session.done,
            session.checkpoint.coerceAtLeast(0L),
            System.currentTimeMillis()
        )
    }

    private class Carry(var histogram: FloatArray?, var cursor: Int)

    private suspend fun collectFrames(
        request: ExtractRequest,
        session: FfmpegSession,
        processor: FfmpegFrameProcessor,
        batch: MutableList<FrameEntity>,
        tally: ProgressTally
    ) {
        val carry = Carry(if (session.done > 0) ResumeReference.load(frameDao, request.projectId, request.mode) else null, session.startIndex)
        coroutineScope {
            val queue = Channel<FfmpegStaged>(Channel.RENDEZVOUS, onUndeliveredElement = { discard(it) })
            launch(Dispatchers.IO) {
                for (staged in queue) {
                    try {
                        consume(request, session, processor, staged, batch, tally)
                    } catch (e: Throwable) {
                        discard(staged)
                        throw e
                    }
                }
            }
            try {
                while (true) {
                    val view = session.index.state.value
                    tally.retarget(scaleCandidates(session.count(session.startIndex), view, session.startUs, session.endUs, session.durationUs))
                    pass(request, session, processor, queue, carry)
                    if (view.complete || (session.endUs != Long.MAX_VALUE && view.lastPtsUs >= session.endUs)) break
                    awaitGrowth(session, view)
                }
            } finally {
                queue.close()
            }
        }
    }

    private suspend fun pass(
        request: ExtractRequest,
        session: FfmpegSession,
        processor: FfmpegFrameProcessor,
        queue: Channel<FfmpegStaged>,
        carry: Carry
    ) {
        session.select(carry.cursor).collect { index ->
            if (isCancelled() || !coroutineContext.isActive) throw CancellationException()
            while (isPaused() && !isCancelled()) delay(PAUSE_POLL_MS)
            val staged = processor.prepare(request, session, index, carry.histogram)
            when (staged) {
                is FfmpegStaged.Frame -> carry.histogram = staged.histogram
                is FfmpegStaged.Idle -> if (staged.tracked) carry.histogram = staged.histogram
            }
            queue.send(staged)
            carry.cursor = index
        }
    }

    private suspend fun awaitGrowth(session: FfmpegSession, seen: IndexView) {
        while (session.index.state.value == seen) {
            if (isCancelled() || !coroutineContext.isActive) throw CancellationException()
            delay(PAUSE_POLL_MS)
        }
    }

    private fun discard(staged: FfmpegStaged) {
        if (staged is FfmpegStaged.Frame && !staged.bitmap.isRecycled) staged.bitmap.recycle()
    }

    private suspend fun consume(
        request: ExtractRequest,
        session: FfmpegSession,
        processor: FfmpegFrameProcessor,
        staged: FfmpegStaged,
        batch: MutableList<FrameEntity>,
        tally: ProgressTally
    ) {
        tally.advance()
        session.lastIndex = staged.index
        if (staged is FfmpegStaged.Frame) {
            accept(session, batch, processor.write(request, staged))
            if (batch.size >= BATCH_SIZE || (batch.isNotEmpty() && flushClock.due()) || (spaceCheckDue() && !storage.hasHeadroom())) flush(request.projectId, session, batch)
        }
        tally.persist(projectDao, request.projectId, session.done)
        onProgress(session.done, tally.total(session.done), session.lastPts)
    }

    private fun spaceCheckDue(): Boolean {
        if (++sinceSpaceCheck < SPACE_CHECK_EVERY) return false
        sinceSpaceCheck = 0
        return true
    }

    private fun accept(session: FfmpegSession, batch: MutableList<FrameEntity>, entity: FrameEntity) {
        batch += entity
        session.done++
        session.lastPts = entity.ptsUs
    }

    private suspend fun flush(projectId: Long, session: FfmpegSession, batch: MutableList<FrameEntity>) {
        checkpointStore.commit(
            projectId, ProjectStatus.Extracting, session.done, session.lastPts, session.lastIndex, batch.toList()
        )
        batch.clear()
        if (!storage.hasHeadroom()) throw LowSpaceException()
    }

    private suspend fun commit(
        request: ExtractRequest,
        session: FfmpegSession,
        batch: List<FrameEntity>,
        status: ProjectStatus
    ) {
        checkpointStore.commit(
            request.projectId, status, session.done, session.lastPts, session.lastIndex, batch.toList()
        )
    }

    private fun interruptedStatus(): ProjectStatus =
        if (isCancelled()) ProjectStatus.Cancelled else ProjectStatus.Paused

    private fun finalStatus(): ProjectStatus = when {
        isCancelled() -> ProjectStatus.Cancelled
        isPaused() -> ProjectStatus.Paused
        else -> ProjectStatus.Completed
    }

    private companion object {
        const val BATCH_SIZE = 50
        const val SPACE_CHECK_EVERY = 8
        const val PAUSE_POLL_MS = 100L
        const val CANCEL_POLL_MS = 50L
    }
}
