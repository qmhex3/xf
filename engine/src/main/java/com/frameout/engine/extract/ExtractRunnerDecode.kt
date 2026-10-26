package com.frameout.engine.extract

import android.graphics.Bitmap
import com.frameout.codecpack.MediaFailure
import com.frameout.core.util.DiagLog
import com.frameout.engine.decode.FrameDecoder
import com.frameout.engine.decode.FramePuller
import com.frameout.engine.index.SampleInfo

internal suspend fun ExtractRunner.decode(request: ExtractRequest, sample: SampleInfo, decoder: FrameDecoder, state: LoopState): Staged {
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
    if (decoder.consecutiveFallbacks >= RUNNER_FORMAT_RETRY_AFTER && decoder.reopenWithoutP010()) state.position = -1
    try { escalateIfSlow(context, decoder, sample) } catch (e: FrameDecodeException) { bitmap.recycle(); throw e }
    return admit(state, sample, mode, byLuma, signature, pts, bitmap)
}

internal suspend fun ExtractRunner.admit(state: LoopState, sample: SampleInfo, mode: ExtractMode, byLuma: Boolean, pulledSignature: FloatArray?, pts: Long, bitmap: Bitmap): Staged {
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

internal fun ExtractRunner.isRepeat(state: LoopState, pts: Long): Boolean = pts == state.lastPulledPts

internal fun ExtractRunner.repeated(state: LoopState, sample: SampleInfo): Staged {
    state.consecutiveFailures = 0
    com.frameout.core.util.DiagLog.append("extract", "sample ${sample.index} resolved to an already extracted frame, skipped")
    return Staged.Idle(sample)
}

internal fun ExtractRunner.reject(state: LoopState, mode: ExtractMode, signature: FloatArray?, sample: SampleInfo): Staged {
    state.consecutiveFailures = 0
    state.histogram = SceneChangeSelector.carry(mode, state.histogram, signature)
    return Staged.Idle(sample)
}

internal fun ExtractRunner.pullWithRetry(decoder: FrameDecoder, sample: SampleInfo, state: LoopState): Pair<Long, Bitmap> {
    val first = runCatching { FramePuller.pull(decoder, sample.ptsUs) }
    first.getOrNull()?.let { found ->
        if (found.first == sample.ptsUs || state.driftMisses >= RUNNER_MAX_DRIFT_MISSES) return found
        return realign(decoder, sample, state, found)
    }
    runCatching { decoder.seekToKeyframe(sample.ptsUs) }
    val second = runCatching { FramePuller.pull(decoder, sample.ptsUs) }
    second.getOrNull()?.let { return it }
    throwDecodeFailure(sample, first, second)
}

internal fun ExtractRunner.realign(decoder: FrameDecoder, sample: SampleInfo, state: LoopState, found: Pair<Long, Bitmap>): Pair<Long, Bitmap> {
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
