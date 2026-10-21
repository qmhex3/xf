package com.frameout.engine.decode

import com.frameout.codecpack.FrameBudget

internal object DecodeBudget {
    private const val MAX_TAPS = 4

    fun step(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var step = 1
        while (step < 64 && exceeds(width.toLong() / step, height.toLong() / step)) step++
        return step
    }

    fun scaled(size: Int, step: Int): Int = ((size + step - 1) / step).coerceAtLeast(1)

    fun taps(step: Int): Int = step.coerceIn(1, MAX_TAPS)

    fun sourceIndex(index: Int, step: Int, tap: Int, taps: Int, limit: Int): Int =
        (index * step + ((2 * tap + 1) * step) / (2 * taps)).coerceIn(0, (limit - 1).coerceAtLeast(0))

    private fun exceeds(w: Long, h: Long): Boolean = w * h > FrameBudget.MAX_PIXELS || maxOf(w, h) > FrameBudget.MAX_SIDE
}
