package com.frameout.engine.extract

import com.frameout.codecpack.CodecPack
import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureClassifier
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.net.Uri
import com.frameout.core.db.FrameDao
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectDao
import com.frameout.core.model.ProjectStatus
import com.frameout.core.storage.StorageManager
import com.frameout.engine.decode.FrameDecoder
import com.frameout.engine.decode.FramePuller
import com.frameout.engine.decode.RawOutput
import com.frameout.engine.index.SampleInfo
import com.frameout.core.util.DeviceLimits
import com.frameout.core.util.DiagLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class ExtractRunner(
    private val context: Context, private val projectDao: ProjectDao, private val frameDao: FrameDao,
    private val storage: StorageManager, private val checkpointStore: CheckpointStore, private val isCancelled: () -> Boolean,
    private val isPaused: () -> Boolean, private val onProgress: (Int, Int, Long) -> Unit
) {
    private val writer = SampleWriter(storage)
    private val flushClock = FlushClock()
    private var gate: com.frameout.core.quality.ExtractQualityGate? = null
    private val timer = StageTimer()
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

    private fun logTiming(state: LoopState) {
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
            pixels > LARGE_FRAME_PIXELS -> Channel.RENDEZVOUS
            pixels > MEDIUM_FRAME_PIXELS -> 1
            else -> 2
        }
    }

    private fun discard(staged: Staged) {
        if (staged is Staged.Frame && !staged.bitmap.isRecycled) staged.bitmap.recycle()
    }

    private class Pending(val staged: Staged, val job: Deferred<SampleResult>?)

    private fun writerLimit(pixels: Long): Int {
        val bySize = when {
            pixels > LARGE_FRAME_PIXELS -> 2
            pixels > MEDIUM_FRAME_PIXELS -> 3
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
            if (batch.size >= 50 || (batch.isNotEmpty() && flushClock.due()) || (++state.sinceSpaceCheck >= SPACE_CHECK_EVERY && !storage.hasHeadroom().also { state.sinceSpaceCheck = 0 })) flush(request.projectId, state.done, state.lastPts, state.lastSampleIndex, batch)
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
        if (e.reason.fatal || state.consecutiveFailures >= MAX_CONSECUTIVE_FAILURES || state.skipped > maxOf(MIN_SKIP_BUDGET, total / SKIP_BUDGET_DIVISOR)) throw e
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

    private suspend fun persistInterrupted(request: ExtractRequest, state: LoopState, batch: List<FrameEntity>) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            logTiming(state)
            if (batch.isNotEmpty()) checkpointStore.commit(request.projectId, ProjectStatus.Paused, state.done, state.lastPts, state.lastSampleIndex, batch.toList())
            projectDao.updateCheckpoint(request.projectId, if (isCancelled()) ProjectStatus.Cancelled.name else ProjectStatus.Paused.name, state.done, state.lastPts.coerceAtLeast(0L), state.lastSampleIndex, System.currentTimeMillis())
        }
    }

    private suspend fun persistFailed(request: ExtractRequest, state: LoopState, batch: List<FrameEntity>) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            runCatching {
                if (batch.isNotEmpty()) checkpointStore.commit(request.projectId, ProjectStatus.Extracting, state.done, state.lastPts, state.lastSampleIndex, batch.toList())
            }
        }
    }

    private suspend fun finish(request: ExtractRequest, state: LoopState, batch: MutableList<FrameEntity>) {
        val status = if (isCancelled()) ProjectStatus.Cancelled else ProjectStatus.Completed
        timer.time(StageTimer.Stage.Commit) { checkpointStore.commit(request.projectId, status, state.done, state.lastPts, state.lastSampleIndex, batch.toList()) }
        logTiming(state)
        if (status == ProjectStatus.Completed) projectDao.setTargetCount(request.projectId, state.done)
    }

    private suspend fun ensureRunning() = awaitRunnable(isCancelled, isPaused)
    private suspend fun decode(request: ExtractRequest, sample: SampleInfo, decoder: FrameDecoder, state: LoopState): Staged {
        val mode = request.mode
        val byLuma = SceneChangeSelector.usesLuma(mode)
        var signature: FloatArray? = null
        val pts: Long
        val bitmap: Bitmap
        if (byLuma) {
            val raw = pullRawWithRetry(decoder, sample)
            if (isRepeat(state, raw.ptsUs)) {
                decoder.drop(raw)
                return repeated(state, sample)
            }
            state.lastPulledPts = raw.ptsUs
            signature = decoder.withLuma(raw) { w, h, grid -> SceneChangeSelector.signature(mode, w, h, grid) }
            if (signature != null && !SceneChangeSelector.changed(mode, state.histogram, signature)) {
                decoder.drop(raw)
                return reject(state, mode, signature, sample)
            }
            val converted = decoder.convert(raw) ?: throw FrameDecodeException(sample.index, "Frame decode failed at sample ${sample.index}", MediaFailure.CorruptData)
            pts = converted.first
            bitmap = converted.second
        } else {
            val decoded = pullWithRetry(decoder, sample, state)
            if (isRepeat(state, decoded.first)) {
                decoded.second.recycle()
                return repeated(state, sample)
            }
            state.lastPulledPts = decoded.first
            pts = decoded.first
            bitmap = decoded.second
        }
        if (decoder.consecutiveFallbacks >= FORMAT_RETRY_AFTER && decoder.reopenWithoutP010()) state.position = -1
        try { escalateIfSlow(context, decoder, sample) } catch (e: FrameDecodeException) { bitmap.recycle(); throw e }
        return admit(state, sample, mode, byLuma, signature, pts, bitmap)
    }

    private suspend fun admit(state: LoopState, sample: SampleInfo, mode: ExtractMode, byLuma: Boolean, pulledSignature: FloatArray?, pts: Long, bitmap: Bitmap): Staged {
        var signature = pulledSignature
        if (byLuma && signature == null) {
            signature = SceneChangeSelector.signature(mode, bitmap)
            if (signature != null && !SceneChangeSelector.changed(mode, state.histogram, signature)) {
                bitmap.recycle()
                return reject(state, mode, signature, sample)
            }
        }
        if (gate?.acceptAsync(bitmap) == false) {
            bitmap.recycle()
            return reject(state, mode, signature, sample)
        }
        state.consecutiveFailures = 0
        state.histogram = signature
        return Staged.Frame(sample, pts, bitmap)
    }

    private fun isRepeat(state: LoopState, pts: Long): Boolean = pts == state.lastPulledPts

    private fun repeated(state: LoopState, sample: SampleInfo): Staged {
        state.consecutiveFailures = 0
        com.frameout.core.util.DiagLog.append("extract", "sample ${sample.index} resolved to an already extracted frame, skipped")
        return Staged.Idle(sample)
    }

    private fun reject(state: LoopState, mode: ExtractMode, signature: FloatArray?, sample: SampleInfo): Staged {
        state.consecutiveFailures = 0
        state.histogram = SceneChangeSelector.carry(mode, state.histogram, signature)
        return Staged.Idle(sample)
    }

    private fun pullWithRetry(decoder: FrameDecoder, sample: SampleInfo, state: LoopState): Pair<Long, Bitmap> {
        val first = runCatching { FramePuller.pull(decoder, sample.ptsUs) }
        first.getOrNull()?.let { found ->
            if (found.first == sample.ptsUs || state.driftMisses >= MAX_DRIFT_MISSES) return found
            return realign(decoder, sample, state, found)
        }
        runCatching { decoder.seekToKeyframe(sample.ptsUs) }
        val second = runCatching { FramePuller.pull(decoder, sample.ptsUs) }
        second.getOrNull()?.let { return it }
        throwDecodeFailure(sample, first, second)
    }

    private fun realign(decoder: FrameDecoder, sample: SampleInfo, state: LoopState, found: Pair<Long, Bitmap>): Pair<Long, Bitmap> {
        runCatching { decoder.seekToKeyframe(sample.ptsUs) }
        val retry = runCatching { FramePuller.pull(decoder, sample.ptsUs) }.getOrNull()
        state.position = -1
        if (retry == null) {
            state.driftMisses++
            return found
        }
        val foundGap = kotlin.math.abs(found.first - sample.ptsUs)
        val retryGap = kotlin.math.abs(retry.first - sample.ptsUs)
        return if (retryGap < foundGap) {
            state.driftMisses = 0
            found.second.recycle()
            retry
        } else {
            state.driftMisses++
            retry.second.recycle()
            found
        }
    }
    private data class LoopState(var done: Int, var lastPts: Long, var histogram: FloatArray?, var position: Int, var thumbnailSet: Boolean, var lastSampleIndex: Int, var lastPulledPts: Long, var consecutiveFailures: Int = 0, var skipped: Int = 0, var sinceSpaceCheck: Int = 0, var driftMisses: Int = 0)
    private companion object {
        const val MAX_CONSECUTIVE_FAILURES = 25
        const val MIN_SKIP_BUDGET = 100
        const val SKIP_BUDGET_DIVISOR = 5
        const val SPACE_CHECK_EVERY = 8
        const val FORMAT_RETRY_AFTER = 2
        const val MAX_DRIFT_MISSES = 3
        const val LARGE_FRAME_PIXELS = 8_000_000L
        const val MEDIUM_FRAME_PIXELS = 2_500_000L
    }
    private suspend fun flush(projectId: Long, done: Int, pts: Long, sampleIndex: Int, batch: MutableList<FrameEntity>) {
        timer.time(StageTimer.Stage.Commit) { checkpointStore.commit(projectId, ProjectStatus.Extracting, done, pts, sampleIndex, batch.toList()) }
        batch.clear()
        if (!storage.hasHeadroom()) throw LowSpaceException()
    }
}
