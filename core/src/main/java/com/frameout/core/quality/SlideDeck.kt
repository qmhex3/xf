package com.frameout.core.quality

import com.frameout.core.db.FrameDao

class SlideCandidate(val id: Long, val ptsUs: Long, val signature: FloatArray, val score: Double)

object SlideDeck {
    const val MIN_SLIDE_US = 750_000L

    fun unique(frames: List<SlideCandidate>, dropTransitions: Boolean): List<Long> {
        val sorted = frames.sortedBy { it.ptsUs }
        if (sorted.size <= 1) return sorted.map { it.id }
        val stable = if (dropTransitions) settled(sorted) else sorted
        val firsts = ArrayList<SlideCandidate>()
        val bests = ArrayList<SlideCandidate>()
        for (frame in stable) {
            val slot = firsts.indexOfFirst { !SlideSignature.differs(it.signature, frame.signature) }
            if (slot < 0) {
                firsts += frame
                bests += frame
            } else if (frame.score >= bests[slot].score) {
                bests[slot] = frame
            }
        }
        return bests.map { it.id }
    }

    private fun settled(sorted: List<SlideCandidate>): List<SlideCandidate> {
        val kept = sorted.filterIndexed { i, frame -> i == sorted.lastIndex || sorted[i + 1].ptsUs - frame.ptsUs >= MIN_SLIDE_US }
        return kept.ifEmpty { sorted }
    }

    suspend fun uniqueIds(dao: FrameDao, projectId: Long, dropTransitions: Boolean): List<Long> =
        unique(
            FrameScan.mapThumbnails(dao, projectId) { frame, bitmap ->
                val luma = Sharpness.lumaOf(bitmap)
                SlideCandidate(frame.id, frame.ptsUs, SlideSignature.of(bitmap), Sharpness.score(luma, bitmap.width, bitmap.height))
            },
            dropTransitions
        )
}
