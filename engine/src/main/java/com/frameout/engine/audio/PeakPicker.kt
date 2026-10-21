package com.frameout.engine.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object PeakPicker {
    const val SILENCE_FLOOR = 0.02f
    private const val SPREAD = 2f

    fun pick(levels: FloatArray, windowMs: Long, maxPeaks: Int, minGapMs: Long): List<Long> {
        if (levels.size < 3 || maxPeaks <= 0 || windowMs <= 0) return emptyList()
        val limit = threshold(levels)
        val gap = (minGapMs / windowMs).toInt().coerceAtLeast(1)
        val ranked = (1 until levels.size - 1).filter { isPeak(levels, it, limit) }.sortedByDescending { levels[it] }
        val chosen = ArrayList<Int>()
        for (index in ranked) {
            if (chosen.size >= maxPeaks) break
            if (chosen.none { abs(it - index) < gap }) chosen.add(index)
        }
        return chosen.sorted().map { it * windowMs }
    }

    private fun isPeak(levels: FloatArray, i: Int, limit: Float): Boolean =
        levels[i] >= limit && levels[i] >= levels[i - 1] && levels[i] > levels[i + 1]

    private fun threshold(levels: FloatArray): Float {
        val mean = levels.average().toFloat()
        var variance = 0f
        levels.forEach { variance += (it - mean) * (it - mean) }
        return max(mean + SPREAD * sqrt(variance / levels.size), SILENCE_FLOOR)
    }
}
