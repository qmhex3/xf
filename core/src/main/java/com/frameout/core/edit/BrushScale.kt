package com.frameout.core.edit

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object BrushScale {
    const val DRAW_MIN = 0.0002f
    const val DRAW_MAX = 0.12f
    const val COVER_MIN = 0.002f
    const val COVER_MAX = 0.3f

    fun toFraction(size: Float, lo: Float, hi: Float): Float =
        (ln(size.coerceIn(lo, hi) / lo) / ln(hi / lo)).coerceIn(0f, 1f)

    fun fromFraction(fraction: Float, lo: Float, hi: Float): Float =
        lo * (hi / lo).pow(fraction.coerceIn(0f, 1f))

    fun pixels(size: Float, width: Int, height: Int): Int =
        max(1, (size * min(width, height)).roundToInt())

    fun stepPixels(size: Float, direction: Int, width: Int, height: Int, lo: Float, hi: Float): Float {
        val shorter = min(width, height).coerceAtLeast(1)
        val target = max(1, pixels(size, width, height) + direction)
        return (target.toFloat() / shorter).coerceIn(lo, hi)
    }
}
