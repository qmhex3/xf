package com.frameout.engine.extract

import android.content.Context
import android.net.Uri
import com.frameout.core.db.FrameDao
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectDao
import com.frameout.core.model.ProjectStatus
import com.frameout.core.storage.StorageManager
import com.frameout.core.util.DeviceLimits
import com.frameout.core.util.DiagLog
import com.frameout.engine.decode.FrameDecoder
import com.frameout.engine.index.SampleInfo
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExtractRunner(
    internal val context: Context, internal val projectDao: ProjectDao, private val frameDao: FrameDao,
    private val storage: StorageManager, internal val checkpointStore: CheckpointStore, internal val isCancelled: () -> Boolean,
    internal val isPaused: () -> Boolean, private val onProgress: (Int, Int, Long) -> Unit
) {
    private val writer = SampleWriter(storage)
    private val flushClock = FlushClock()
    internal var gate: com.frameout.core.quality.ExtractQualityGate? = null
    internal val timer = StageTimer()
    @Volatile private var decodersUsed = 1
    @Volatile private var writersPeak = 1

    suspend fun run(request: ExtractRequest, uri: Uri, selected: Flow<SampleInfo>, total: Int, checkpoint: Long) {
        com.frameout.core.util.RuntimeGate.pass()
        val project = projectDao.getById(request.projectId)
        val existing = frameDao.countAll(request.projectId)
        gate = GateResume.gate(request, frameDao, existing > 0)
        if (total <= existing) {
            projectDao.updateProgress(request.projectId, ProjectStatus.Completed.name, existing, checkpoint.coerceAtLeast(0L), System.currentTimeMillis())
            onProgress(existing, total, checkpoint)
            return
        }
        projectDao.setTargetCount(request.projectId, total)
        val decoder = FrameDecoder(context)
        val resumedPts = project?.lastPtsUs ?: checkpoint
        val state = LoopState(existing, resumedPts, if (existing > 0) ResumeReference.load(frameDao, request.projectId, request.mode) else null, -1, project?.thumbnailPath != null, project?.lastSampleIndex ?: -1, if (existing > 0) resumedPts else -1L)
        val tally = ProgressTally(total - existing, existing)
        try {
            decoder.open(uri)
            if (!storage.hasSpaceForResolution(SpaceEstimate.frames(request.mode, total - existing), decoder.videoWidth(), decoder.videoHeight(), request.autoSave, request.saveFormat)) {
                throw LowSpaceException()
            }
            val batch = extractSamples(request, uri, selected, total, decoder, state, tally)
            finish(request, state, batch)
        } finally {
            decoder.close()
        }
    }

    internal fun logTiming(state: LoopState) {
        DiagLog.append("extract", timer.summary(state.done, decodersUsed, writersPeak))
    }

    private suspend fun extractSamples(request: ExtractRequest, uri: Uri, selected: Flow<SampleInfo>, total: Int, decoder: FrameDecoder, state: LoopState, tally: ProgressTally): MutableList<FrameEntity> {
        val batch = mutableListOf<FrameEntity>()
        val extras = mutableListOf<FrameDecoder>()
        try {
            coroutineScope {
                val queue = Channel<Staged>(queueCapacity(decoder), onUndeliveredElement = { discard(it) })
                val pixels = decoder.videoWidth().toLong() * decoder.videoHeight()
                launch(Dispatchers.IO) { drain(request, queue, state, batch, tally, pixels) }
                try {
                    val wanted = if (ParallelPlan.eligible(request.mode, gate != null)) ParallelPlan.decoders(pixels, DeviceLimits.extractDecoders()) else 1
                    if (wanted > 1) openExtras(uri, wanted - 1, extras)
                    if (extras.isEmpty()) {
                        selected.collect { sample ->
                            ensureRunning()
                            queue.send(timer.time(StageTimer.Stage.Decode) { stage(request, sample, total, decoder, state) })
                        }
                    } else {
                        decodersUsed = extras.size + 1
                        produceParallel(request, selected, total, listOf(decoder) + extras, state, queue)
                    }
                } finally {
                    queue.close()
                }
            }
        } catch (e: CancellationException) {
            persistInterrupted(request, state, batch)
            throw e
        } catch (e: Throwable) {
            persistFailed(request, state, batch)
            throw e
        } finally {
            extras.forEach { runCatching { it.close() } }
        }
        if (!coroutineContext.isActive) {
            persistInterrupted(request, state, batch)
            throw CancellationException()
        }
        return batch
    }

    private fun queueCapacity(decoder: FrameDecoder): Int {
        val pixels = decoder.videoWidth().toLong() * decoder.videoHeight()
        return when {
            pixels > RUNNER_LARGE_FRAME_PIXELS -> Channel.RENDEZVOUS
            pixels > RUNNER_MEDIUM_FRAME_PIXELS -> 1
            else -> 2
        }
    }

    private fun discard(staged: Staged) {
        if (staged is Staged.Frame && !staged.bitmap.isRecycled) staged.bitmap.recycle()
    }

    private class Pending(val staged: Staged, val job: Deferred<SampleResult>?)

    private fun writerLimit(pixels: Long): Int {
        val bySize = when {
            pixels > RUNNER_LARGE_FRAME_PIXELS -> 2
            pixels > RUNNER_MEDIUM_FRAME_PIXELS -> 3
            else -> 4
        }
        return minOf(DeviceLimits.extractWriters(), bySize).coerceAtLeast(1)
    }

    private suspend fun drain(request: ExtractRequest, queue: Channel<Staged>, state: LoopState, batch: MutableList<FrameEntity>, tally: ProgressTally, pixels: Long) = coroutineScope {
        val inflight = ArrayDeque<Pending>()
        try {
            for (staged in queue) {
                inflight.addLast(startWrite(request, staged, this))
                val limit = writerLimit(pixels)
                if (limit > writersPeak) writersPeak = limit
                while (inflight.size >= limit) settle(request, inflight.removeFirst(), state, batch, tally)
            }
            while (inflight.isNotEmpty()) settle(request, inflight.removeFirst(), state, batch, tally)
        } catch (e: Throwable) {
            inflight.forEach { it.job?.cancel() }
            throw e
        }
    }

    private fun startWrite(request: ExtractRequest, staged: Staged, scope: CoroutineScope): Pending {
        if (staged !is Staged.Frame) return Pending(staged, null)
        val job = scope.async(Dispatchers.IO, start = CoroutineStart.ATOMIC) {
            timer.time(StageTimer.Stage.Write) { writer.write(request, staged.sample, staged.pts, staged.bitmap, null) }
        }
        return Pending(staged, job)
    }

    private suspend fun settle(request: ExtractRequest, pending: Pending, state: LoopState, batch: MutableList<FrameEntity>, tally: ProgressTally) {
        tally.advance()
        val staged = pending.staged
        if (pending.job != null) {
            val result = pending.job.await()
            recordFrame(request, state, batch, staged.sample, result)
            state.lastSampleIndex = staged.sample.index
            if (batch.size >= 50 || (batch.isNotEmpty() && flushClock.due()) || (++state.sinceSpaceCheck >= RUNNER_SPACE_CHECK_EVERY && !storage.hasHeadroom().also { state.sinceSpaceCheck = 0 })) flush(request.projectId, state.done, state.lastPts, state.lastSampleIndex, batch)
        } else {
            state.lastSampleIndex = staged.sample.index
        }
        tally.persist(projectDao, request.projectId, state.done)
        onProgress(state.done, tally.total(state.done), state.lastPts)
    }

    private suspend fun openExtras(uri: Uri, count: Int, into: MutableList<FrameDecoder>) {
        repeat(count) {
            val extra = FrameDecoder(context)
            try {
                extra.open(uri)
                into += extra
            } catch (e: CancellationException) {
                runCatching { extra.close() }
                throw e
            } catch (e: Throwable) {
                runCatching { extra.close() }
                DiagLog.append("extract", "extra decoder unavailable, continuing with ${into.size + 1}: ${e.message}")
                return
            }
        }
    }

    private suspend fun produceParallel(request: ExtractRequest, selected: Flow<SampleInfo>, total: Int, decoders: List<FrameDecoder>, state: LoopState, queue: Channel<Staged>) = coroutineScope {
        val count = decoders.size
        val inputs = List(count) { Channel<SampleInfo>(1) }
        val outputs = List(count) { Channel<Staged>(1, onUndeliveredElement = { discard(it) }) }
        val order = Channel<Int>(Channel.UNLIMITED)
        decoders.forEachIndexed { i, worker ->
            val local = LoopState(0, -1L, null, -1, true, -1, -1L)
            launch(Dispatchers.IO) {
                try {
                    for (sample in inputs[i]) outputs[i].send(timer.time(StageTimer.Stage.Decode) { stage(request, sample, total, worker, local) })
                } finally {
                    outputs[i].close()
                }
            }
        }
        launchOrdered(order, outputs, state, queue)
        try {
            var ordinal = 0L
            selected.collect { sample ->
                ensureRunning()
                val active = DeviceLimits.extractDecoders().coerceIn(1, count)
                val target = (ordinal++ % active).toInt()
                order.send(target)
                inputs[target].send(sample)
            }
        } finally {
            order.close()
            inputs.forEach { it.close() }
        }
    }

    private fun CoroutineScope.launchOrdered(order: Channel<Int>, outputs: List<Channel<Staged>>, state: LoopState, queue: Channel<Staged>) = launch {
        var lastPts = state.lastPulledPts
        for (index in order) {
            val staged = outputs[index].receiveCatching().getOrNull() ?: return@launch
            val next = if (staged is Staged.Frame && staged.pts == lastPts) {
                discard(staged)
                DiagLog.append("extract", "sample ${staged.sample.index} resolved to an already extracted frame, skipped")
                Staged.Idle(staged.sample)
            } else {
                if (staged is Staged.Frame) lastPts = staged.pts
                staged
            }
            queue.send(next)
        }
    }

    private suspend fun stage(request: ExtractRequest, sample: SampleInfo, total: Int, decoder: FrameDecoder, state: LoopState): Staged {
        if (SeekPolicy.needsSeek(sample, state.position)) decoder.seekToKeyframe(sample.ptsUs)
        state.position = sample.index
        return try {
            decode(request, sample, decoder, state)
        } catch (e: FrameDecodeException) {
            registerSkip(state, sample, e, total)
            Staged.Idle(sample)
        }
    }

    private fun registerSkip(state: LoopState, sample: SampleInfo, e: FrameDecodeException, total: Int) {
        state.position = -1
        state.consecutiveFailures++
        state.skipped++
        com.frameout.core.util.DiagLog.append("extract", "skipped sample ${sample.index}: ${e.message}")
        if (e.reason.fatal || state.consecutiveFailures >= RUNNER_MAX_CONSECUTIVE_FAILURES || state.skipped > maxOf(RUNNER_MIN_SKIP_BUDGET, total / RUNNER_SKIP_BUDGET_DIVISOR)) throw e
    }

    private suspend fun recordFrame(request: ExtractRequest, state: LoopState, batch: MutableList<FrameEntity>, sample: SampleInfo, result: SampleResult) {
        val frame = result.frame ?: return
        batch += frame
        state.done++
        state.lastPts = frame.ptsUs
        state.lastSampleIndex = sample.index
        if (!state.thumbnailSet) {
            projectDao.setThumbnail(request.projectId, frame.thumbnailPath, System.currentTimeMillis())
            state.thumbnailSet = true
        }
    }

    private suspend fun flush(projectId: Long, done: Int, pts: Long, sampleIndex: Int, batch: MutableList<FrameEntity>) {
        timer.time(StageTimer.Stage.Commit) { checkpointStore.commit(projectId, ProjectStatus.Extracting, done, pts, sampleIndex, batch.toList()) }
        batch.clear()
        if (!storage.hasHeadroom()) throw LowSpaceException()
    }
}
