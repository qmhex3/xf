package com.frameout.engine.decode

import android.graphics.Bitmap

object FramePuller {
    private const val MAX_TRAILING_GAP_US = 1_000_000L

    fun pull(decoder: FrameDecoder, targetPts: Long, maxFrames: Int = Int.MAX_VALUE, strict: Boolean = false): Pair<Long, Bitmap>? {
        var before: RawOutput? = null
        var seen = 0
        var exhausted = false
        while (true) {
            if (seen++ >= maxFrames) { exhausted = true; break }
            val raw = decoder.nextOutput() ?: break
            if (raw.ptsUs < targetPts) { before?.let(decoder::drop); before = raw; continue }
            val previous = before ?: return decoder.convert(raw)
            return choose(decoder, previous, raw, targetPts)
        }
        if (exhausted && strict) {
            before?.let(decoder::drop)
            return null
        }
        val last = before ?: return null
        if (targetPts - last.ptsUs > MAX_TRAILING_GAP_US) {
            decoder.drop(last)
            return null
        }
        return decoder.convert(last)
    }

    internal fun pullRaw(decoder: FrameDecoder, targetPts: Long): RawOutput? {
        var before: RawOutput? = null
        while (true) {
            val raw = decoder.nextOutput() ?: break
            if (raw.ptsUs < targetPts) { before?.let(decoder::drop); before = raw; continue }
            val previous = before ?: return raw
            val keepBefore = targetPts - previous.ptsUs <= raw.ptsUs - targetPts
            decoder.drop(if (keepBefore) raw else previous)
            return if (keepBefore) previous else raw
        }
        val last = before ?: return null
        if (targetPts - last.ptsUs > MAX_TRAILING_GAP_US) {
            decoder.drop(last)
            return null
        }
        return last
    }

    private fun choose(decoder: FrameDecoder, before: RawOutput, after: RawOutput, targetPts: Long): Pair<Long, Bitmap>? {
        val keepBefore = targetPts - before.ptsUs <= after.ptsUs - targetPts
        val kept = if (keepBefore) before else after
        decoder.drop(if (keepBefore) after else before)
        return decoder.convert(kept)
    }
}
