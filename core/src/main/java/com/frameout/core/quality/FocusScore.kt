package com.frameout.core.quality

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object FocusScore {
    const val MIN_SIDE = 12

    const val PLAIN_CONTRAST = 5.0

    private const val CELL = 16
    private const val MAX_GRID = 8
    private const val NOISE_GAIN = 24.0 // E[Sobel gx^2 + gy^2] for white noise of variance s^2 is 24 s^2
    private const val IMMERKAER = 1.2533141373155 / 6.0 // sqrt(pi / 2) / 6
    private const val TOP_SHARE = 0.25
    private const val TOP_WEIGHT = 0.7
    private const val EXPOSURE_OFFSET = 12.0
    private const val EXPOSURE_BASE = 128.0
    private const val DIRECTION_FLOOR = 0.85

    class Detail(
        val score: Double,
        val noise: Double,
        val contrast: Double,
        val balance: Double
    ) {
        val plain: Boolean get() = contrast < PLAIN_CONTRAST
    }

    private val EMPTY = Detail(0.0, 0.0, 0.0, 1.0)

    fun measure(luma: IntArray, width: Int, height: Int): Double = detail(luma, width, height).score

    fun detail(luma: IntArray, width: Int, height: Int): Detail = detail(luma, width, height, BlockGrid.Grid.NONE)

    fun detail(luma: IntArray, width: Int, height: Int, blocks: BlockGrid.Grid): Detail {
        if (width < MIN_SIDE || height < MIN_SIDE || luma.size < width * height) return EMPTY
        val grid = Grid((width / CELL).coerceIn(1, MAX_GRID), (height / CELL).coerceIn(1, MAX_GRID))
        val stats = lumaStats(luma, width * height)
        val skip = Skip(blocks, width, height)
        for (y in 1 until height - 1) if (!skip.rows[y]) accumulateRow(grid, luma, width, height, y, skip.cols)
        val noise = noiseFloor(grid.noiseSum, grid.noiseCount)
        val values = rankedBlocks(grid, NOISE_GAIN * noise * noise)
        if (values.isEmpty()) return Detail(0.0, noise, stats.contrast, 1.0)
        val pooled = pool(values)
        val direction = DIRECTION_FLOOR + (1.0 - DIRECTION_FLOOR) * sqrt(pooled.balance)
        val exposure = (stats.mean + EXPOSURE_OFFSET) / EXPOSURE_BASE
        return Detail(pooled.energy / (exposure * exposure) * direction, noise, stats.contrast, pooled.balance)
    }

    private class Grid(val cols: Int, val rows: Int) {
        val blocks = cols * rows
        val gx2 = DoubleArray(blocks)
        val gy2 = DoubleArray(blocks)
        val count = IntArray(blocks)
        val noiseSum = DoubleArray(blocks)
        val noiseCount = IntArray(blocks)
    }

    private class Skip(blocks: BlockGrid.Grid, width: Int, height: Int) {
        val cols = BooleanArray(width) { blocks.x.straddles(it) }
        val rows = BooleanArray(height) { blocks.y.straddles(it) }
    }

    private class LumaStats(val mean: Double, val contrast: Double)

    private class Pooled(val energy: Double, val balance: Double)

    private fun lumaStats(luma: IntArray, size: Int): LumaStats {
        var sum = 0.0
        var sumSq = 0.0
        for (i in 0 until size) {
            val v = luma[i].toDouble()
            sum += v
            sumSq += v * v
        }
        val n = size.toDouble()
        val mean = sum / n
        return LumaStats(mean, sqrt(max(0.0, sumSq / n - mean * mean)))
    }

    private fun accumulateRow(grid: Grid, luma: IntArray, width: Int, height: Int, y: Int, skipCols: BooleanArray) {
        val by = min(grid.rows - 1, y * grid.rows / height)
        val up = (y - 1) * width
        val mid = y * width
        val down = (y + 1) * width
        for (x in 1 until width - 1) {
            if (skipCols[x]) continue
            val b = by * grid.cols + min(grid.cols - 1, x * grid.cols / width)
            val tl = luma[up + x - 1]
            val tc = luma[up + x]
            val tr = luma[up + x + 1]
            val ml = luma[mid + x - 1]
            val mc = luma[mid + x]
            val mr = luma[mid + x + 1]
            val bl = luma[down + x - 1]
            val bc = luma[down + x]
            val br = luma[down + x + 1]
            val gx = (tr + 2 * mr + br - tl - 2 * ml - bl).toDouble()
            val gy = (bl + 2 * bc + br - tl - 2 * tc - tr).toDouble()
            grid.gx2[b] += gx * gx
            grid.gy2[b] += gy * gy
            grid.count[b]++
            val lap = (tl - 2 * tc + tr - 2 * ml + 4 * mc - 2 * mr + bl - 2 * bc + br).toDouble()
            grid.noiseSum[b] += if (lap < 0) -lap else lap
            grid.noiseCount[b]++
        }
    }

    private fun rankedBlocks(grid: Grid, floor: Double): ArrayList<Block> {
        val values = ArrayList<Block>(grid.blocks)
        for (b in 0 until grid.blocks) {
            if (grid.count[b] == 0) continue
            val c = grid.count[b].toDouble()
            val e = max(0.0, (grid.gx2[b] + grid.gy2[b]) / c - floor)
            values += Block(e, grid.gx2[b] / c, grid.gy2[b] / c)
        }
        values.sortByDescending { it.energy }
        return values
    }

    private fun pool(values: List<Block>): Pooled {
        val top = max(1, ceil(values.size * TOP_SHARE).toInt())
        var topSum = 0.0
        var topX = 0.0
        var topY = 0.0
        for (i in 0 until top) {
            topSum += values[i].energy
            topX += values[i].gx
            topY += values[i].gy
        }
        val allMean = values.sumOf { it.energy } / values.size
        val energy = TOP_WEIGHT * (topSum / top) + (1.0 - TOP_WEIGHT) * allMean
        val hi = max(topX, topY)
        return Pooled(energy, if (hi <= 0.0) 1.0 else min(topX, topY) / hi)
    }

    private class Block(val energy: Double, val gx: Double, val gy: Double)

    private fun noiseFloor(sum: DoubleArray, count: IntArray): Double {
        val estimates = ArrayList<Double>(sum.size)
        for (b in sum.indices) if (count[b] > 0) estimates += IMMERKAER * sum[b] / count[b]
        if (estimates.isEmpty()) return 0.0
        estimates.sort()
        return estimates[(estimates.size - 1) / 4]
    }
}
