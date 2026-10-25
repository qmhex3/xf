package com.frameout.core.quality

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

object BlockGrid {
    const val MIN_PERIOD = 4
    const val MAX_PERIOD = 32
    const val MIN_SIDE = 128

    private const val SMOOTHING = 2.0
    private const val NEAR_BEST = 0.8
    private const val MARGIN = 3
    private const val TAIL = 4
    private const val SAMPLES_PER_PHASE = 8

    class Axis(val period: Int, val phase: Int, val excess: Double) {
        val found: Boolean get() = period > 0

        fun straddles(pixel: Int): Boolean = found && (Math.floorMod(pixel - 1, period) == phase || Math.floorMod(pixel, period) == phase)

        companion object {
            val NONE = Axis(0, 0, 0.0)
        }
    }

    class Grid(val x: Axis, val y: Axis) {
        val strength: Double get() = sqrt(x.excess * y.excess)

        companion object {
            val NONE = Grid(Axis.NONE, Axis.NONE)
        }
    }

    fun detect(luma: IntArray, width: Int, height: Int): Grid {
        if (width < MIN_SIDE || height < MIN_SIDE || luma.size < width * height) return Grid.NONE
        return Grid(axisOf(columnProfile(luma, width, height)), axisOf(rowProfile(luma, width, height)))
    }

    private fun columnProfile(luma: IntArray, width: Int, height: Int): DoubleArray {
        val sums = DoubleArray(width - 1)
        val steps = IntArray(width - 1)
        for (y in 0 until height) {
            val row = y * width
            for (i in steps.indices) steps[i] = abs(luma[row + i] - luma[row + i + 1])
            accumulate(steps, sums)
        }
        return averaged(sums, height)
    }

    private fun rowProfile(luma: IntArray, width: Int, height: Int): DoubleArray {
        val sums = DoubleArray(height - 1)
        val steps = IntArray(height - 1)
        for (x in 0 until width) {
            for (i in steps.indices) steps[i] = abs(luma[i * width + x] - luma[(i + 1) * width + x])
            accumulate(steps, sums)
        }
        return averaged(sums, width)
    }

    private fun accumulate(steps: IntArray, sums: DoubleArray) {
        var around = 0
        for (i in 0..2 * MARGIN) around += steps[i]
        for (i in MARGIN until steps.size - TAIL) {
            sums[i] += steps[i] / (around + SMOOTHING)
            around += steps[i + MARGIN + 1] - steps[i - MARGIN]
        }
    }

    private fun averaged(sums: DoubleArray, lines: Int): DoubleArray {
        for (i in sums.indices) sums[i] /= lines
        return sums
    }

    private fun axisOf(profile: DoubleArray): Axis {
        val usable = profile.size - MARGIN - TAIL
        val limit = min(MAX_PERIOD, usable / SAMPLES_PER_PHASE)
        if (limit < MIN_PERIOD) return Axis.NONE
        val candidates = (MIN_PERIOD..limit).map { excessAt(profile, it) }
        val best = candidates.maxOf { it.excess }
        return candidates.first { it.excess >= NEAR_BEST * best }
    }

    private fun excessAt(profile: DoubleArray, period: Int): Axis {
        val sums = DoubleArray(period)
        val counts = IntArray(period)
        for (i in MARGIN until profile.size - TAIL) {
            sums[i % period] += profile[i]
            counts[i % period]++
        }
        var peak = 0
        var total = 0.0
        for (b in 0 until period) {
            sums[b] = if (counts[b] == 0) 0.0 else sums[b] / counts[b]
            total += sums[b]
            if (sums[b] > sums[peak]) peak = b
        }
        val others = (total - sums[peak]) / (period - 1)
        return Axis(period, peak, sums[peak] - others)
    }
}
