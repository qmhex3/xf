package com.frameout.core.quality

data class ScoredFrame(val id: Long, val index: Int, val score: Double, val faceScore: Float? = null, val sig: ByteArray? = null, val noise: Float = 0f, val speed: Float = 0f, val ptsUs: Long = 0L, val isKeyframe: Boolean = false) {
    val effective: Double get() = (if (faceScore == null) score else score * (0.4 + 1.2 * faceScore)) * MotionEstimate.factor(speed)
}

object KeyframeTie {
    const val MARGIN = 0.01

    fun best(frames: List<ScoredFrame>): ScoredFrame {
        val top = frames.maxBy { it.effective }
        if (top.isKeyframe || top.effective <= 0.0) return top
        val floor = top.effective * (1.0 - MARGIN)
        return frames.filter { it.isKeyframe && it.effective >= floor }.maxByOrNull { it.effective } ?: top
    }
}

object SharpestPicker {
    fun windowFor(count: Int): Int = (count / 50).coerceIn(MIN_WINDOW, MAX_WINDOW)

    fun pick(frames: List<ScoredFrame>): List<Long> {
        if (frames.isEmpty()) return emptyList()
        val winners = frames.sortedBy { it.index }
            .chunked(windowFor(frames.size))
            .map { chunk -> KeyframeTie.best(chunk) }
        return withoutRepeats(winners).map { it.id }
    }

    private fun withoutRepeats(winners: List<ScoredFrame>): List<ScoredFrame> {
        val kept = ArrayList<ScoredFrame>(winners.size)
        for (w in winners) {
            val last = kept.lastOrNull()
            val a = last?.sig
            val b = w.sig
            if (last != null && a != null && b != null && FrameSignature.same(a, b, 1.0, last.noise, w.noise)) {
                if (w.effective > last.effective) kept[kept.size - 1] = w
            } else kept += w
        }
        return kept
    }

    fun candidates(frames: List<ScoredFrame>, perWindow: Int = CANDIDATES): Set<Long> {
        if (frames.isEmpty()) return emptySet()
        return frames.sortedBy { it.index }
            .chunked(windowFor(frames.size))
            .flatMap { chunk -> chunk.sortedByDescending { it.score }.take(perWindow) }
            .map { it.id }
            .toSet()
    }

    private const val CANDIDATES = 3
    private const val MIN_WINDOW = 5
    private const val MAX_WINDOW = 30
}
