package com.frameout.engine.work

class RemainingEstimator {
    private var startMs = -1L
    private var baseDone = 0

    fun update(extracting: Boolean, paused: Boolean, done: Int, total: Int, nowMs: Long): Long {
        if (!extracting || paused) {
            startMs = -1L
            return 0L
        }
        if (startMs < 0L) {
            startMs = nowMs
            baseDone = done
            return 0L
        }
        val advanced = done - baseDone
        val elapsed = nowMs - startMs
        if (advanced < MIN_ADVANCED || elapsed <= 0L) return 0L
        return elapsed * (total - done).coerceAtLeast(0) / advanced
    }

    private companion object {
        const val MIN_ADVANCED = 5
    }
}
