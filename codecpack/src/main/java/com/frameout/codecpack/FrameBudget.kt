package com.frameout.codecpack

object FrameBudget {
    private const val FLOOR_PIXELS = 8_500_000L
    private const val CEILING_PIXELS = 35_389_440L
    private const val BYTES_PER_PIXEL = 12L

    private const val EXACT_BYTES_PER_PIXEL = 6L
    private const val EXACT_HEAP_PERCENT = 60L

    const val MAX_SIDE = 8192

    val MAX_PIXELS: Long by lazy {
        (Runtime.getRuntime().maxMemory() / BYTES_PER_PIXEL).coerceIn(FLOOR_PIXELS, CEILING_PIXELS)
    }

    fun fit(sourceW: Int, sourceH: Int): Pair<Int, Int>? {
        if (sourceW <= 0 || sourceH <= 0) return null
        val limit = MAX_PIXELS
        val area = sourceW.toLong() * sourceH.toLong()
        var scale = if (area > limit) kotlin.math.sqrt(limit.toDouble() / area.toDouble()) else 1.0
        val longest = maxOf(sourceW, sourceH)
        if (longest * scale > MAX_SIDE) scale = MAX_SIDE.toDouble() / longest
        val w = (sourceW * scale).toInt().coerceAtLeast(1)
        val h = (sourceH * scale).toInt().coerceAtLeast(1)
        return w to h
    }

    fun exactPixels(maxMemory: Long = Runtime.getRuntime().maxMemory()): Long =
        (maxMemory / 100L * EXACT_HEAP_PERCENT / EXACT_BYTES_PER_PIXEL).coerceAtMost(CEILING_PIXELS)

    fun fitExact(sourceW: Int, sourceH: Int, maxMemory: Long = Runtime.getRuntime().maxMemory()): Pair<Int, Int>? {
        if (sourceW <= 0 || sourceH <= 0 || maxOf(sourceW, sourceH) > MAX_SIDE) return null
        return if (sourceW.toLong() * sourceH.toLong() <= exactPixels(maxMemory)) sourceW to sourceH else null
    }
}
