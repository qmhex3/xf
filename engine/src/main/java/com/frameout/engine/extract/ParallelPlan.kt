package com.frameout.engine.extract

internal object ParallelPlan {
    private const val SPARSE_MILLIS = 500L
    private const val SPARSE_FRAMES = 30
    private const val LARGE_FRAME_PIXELS = 8_000_000L
    private const val MEDIUM_FRAME_PIXELS = 2_500_000L

    fun eligible(mode: ExtractMode, hasGate: Boolean): Boolean {
        if (hasGate) return false
        return when (mode) {
            ExtractMode.KeyframesOnly, is ExtractMode.FixedCount -> true
            is ExtractMode.EveryNMillis -> mode.millis >= SPARSE_MILLIS
            is ExtractMode.EveryNFrames -> mode.n >= SPARSE_FRAMES
            else -> false
        }
    }

    fun decoders(pixels: Long, deviceLimit: Int): Int {
        val bySize = when {
            pixels > LARGE_FRAME_PIXELS -> 1
            pixels > MEDIUM_FRAME_PIXELS -> 2
            else -> 3
        }
        return minOf(deviceLimit, bySize).coerceAtLeast(1)
    }
}
