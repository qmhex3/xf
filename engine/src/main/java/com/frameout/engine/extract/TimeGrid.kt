package com.frameout.engine.extract

internal object TimeGrid {
    fun advance(target: Long, ptsUs: Long, stepUs: Long): Long {
        val step = stepUs.coerceAtLeast(1L)
        if (ptsUs < target) return safeAdd(target, step)
        val k = (ptsUs - target) / step + 1L
        return if (k > (Long.MAX_VALUE - target) / step) Long.MAX_VALUE else target + k * step
    }

    private fun safeAdd(a: Long, b: Long): Long = if (a > Long.MAX_VALUE - b) Long.MAX_VALUE else a + b
}
