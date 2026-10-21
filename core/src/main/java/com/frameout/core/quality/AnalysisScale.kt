package com.frameout.core.quality

object AnalysisScale {
    fun sampleFor(width: Int, height: Int, side: Int): Int {
        val longSide = maxOf(width, height)
        if (longSide <= 0 || side <= 0) return 1
        var sample = 1
        while (longSide / (sample * 2) >= side) sample *= 2
        return sample
    }

    fun target(width: Int, height: Int, side: Int): Pair<Int, Int> {
        val longSide = maxOf(width, height)
        if (longSide <= side || side <= 0) return width to height
        val w = (width.toLong() * side / longSide).toInt().coerceAtLeast(1)
        val h = (height.toLong() * side / longSide).toInt().coerceAtLeast(1)
        return w to h
    }
}
