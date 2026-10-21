package com.frameout.engine.edit

internal sealed class SampleDecision {
    data object Skip : SampleDecision()
    data object Stop : SampleDecision()
    data class Write(val outPtsUs: Long) : SampleDecision()
}

internal class SampleGate(
    private val range: KeptRange,
    private val baseOutUs: Long,
    private val videoTrack: Int
) {
    private val videoWritten = HashSet<Long>()
    private val lastAudio = HashMap<Int, Long>()
    private var videoPastEnd = 0

    var maxOutUs: Long = baseOutUs
        private set

    fun decide(track: Int, ptsUs: Long, sync: Boolean): SampleDecision {
        if (ptsUs < 0L) return SampleDecision.Stop
        if (track == videoTrack) return decideVideo(ptsUs, sync)
        return decideOther(track, ptsUs)
    }

    private fun decideVideo(ptsUs: Long, sync: Boolean): SampleDecision {
        if (ptsUs >= range.endUs) return pastEnd(sync)
        videoPastEnd = 0
        if (ptsUs < range.startUs) return SampleDecision.Skip
        if (videoWritten.isEmpty() && !sync) return SampleDecision.Skip
        val outPts = outPts(ptsUs)
        if (!videoWritten.add(outPts)) return SampleDecision.Skip
        return accept(outPts)
    }

    private fun pastEnd(sync: Boolean): SampleDecision {
        videoPastEnd++
        if (sync || videoPastEnd > MAX_REORDER_LOOKAHEAD) return SampleDecision.Stop
        return SampleDecision.Skip
    }

    private fun decideOther(track: Int, ptsUs: Long): SampleDecision {
        if (ptsUs < range.startUs || ptsUs >= range.endUs) return SampleDecision.Skip
        val outPts = outPts(ptsUs)
        val previous = lastAudio[track]
        if (previous != null && outPts <= previous) return SampleDecision.Skip
        lastAudio[track] = outPts
        return accept(outPts)
    }

    private fun accept(outPts: Long): SampleDecision {
        maxOutUs = maxOf(maxOutUs, outPts)
        return SampleDecision.Write(outPts)
    }

    private fun outPts(ptsUs: Long): Long = baseOutUs + (ptsUs - range.startUs)

    companion object {
        const val MAX_REORDER_LOOKAHEAD = 32
    }
}
