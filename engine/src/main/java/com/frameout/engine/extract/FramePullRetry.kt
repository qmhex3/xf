package com.frameout.engine.extract

import android.content.Context
import android.util.Log
import com.frameout.codecpack.CodecPack
import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureClassifier
import com.frameout.engine.decode.FrameDecoder
import com.frameout.engine.decode.FramePuller
import com.frameout.engine.decode.RawOutput
import com.frameout.engine.index.SampleInfo

private const val FALLBACK_SWITCH_AFTER = 3

internal fun pullRawWithRetry(decoder: FrameDecoder, sample: SampleInfo): RawOutput {
    val first = runCatching { FramePuller.pullRaw(decoder, sample.ptsUs) }
    first.getOrNull()?.let { return it }
    runCatching { decoder.seekToKeyframe(sample.ptsUs) }
    val second = runCatching { FramePuller.pullRaw(decoder, sample.ptsUs) }
    second.getOrNull()?.let { return it }
    throwDecodeFailure(sample, first, second)
}

internal fun throwDecodeFailure(sample: SampleInfo, first: Result<*>, second: Result<*>): Nothing {
    val cause = second.exceptionOrNull() ?: first.exceptionOrNull()
    Log.w("ExtractRunner", "decode failed at sample ${sample.index} pts=${sample.ptsUs}", cause)
    val reason = cause?.let { MediaFailureClassifier.classify(it) }?.takeIf { it != MediaFailure.Unknown } ?: MediaFailure.CorruptData
    throw FrameDecodeException(sample.index, "Frame decode failed at sample ${sample.index}", reason, cause)
}

internal fun escalateIfSlow(context: Context, decoder: FrameDecoder, sample: SampleInfo) {
    if (decoder.consecutiveFallbacks < FALLBACK_SWITCH_AFTER || !CodecPack.isInstalled(context)) return
    throw FrameDecodeException(
        sample.index,
        "System decoder output not convertible for ${decoder.consecutiveFallbacks} consecutive frames, switching to FFmpeg",
        MediaFailure.UnsupportedFeature
    )
}
