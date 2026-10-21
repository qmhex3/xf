package com.frameout.engine.extract

class FlushClock(private val intervalMs: Long = DEFAULT_INTERVAL_MS) {
    private var last = System.nanoTime()

    fun due(): Boolean {
        val now = System.nanoTime()
        if ((now - last) / NANOS_PER_MS < intervalMs) return false
        last = now
        return true
    }

    private companion object {
        const val DEFAULT_INTERVAL_MS = 400L
        const val NANOS_PER_MS = 1_000_000L
    }
}
