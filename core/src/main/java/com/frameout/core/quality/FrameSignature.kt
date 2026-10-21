package com.frameout.core.quality

import kotlin.math.abs

object FrameSignature {
    const val COLS = 32
    const val ROWS = 32
    const val SIZE = COLS * ROWS

    const val MAX_MEAN_DIFF = 1.6
    const val CELL_CHANGED = 6.0
    const val MAX_CHANGED_SHARE = 0.02
    const val MAX_CELL_DIFF = 22.0

    class Gap(val meanDiff: Double, val maxDiff: Double, val changedShare: Double)

    fun cellNoise(sigma: Double, width: Int, height: Int): Float {
        val pixels = maxOf(1, width / COLS) * maxOf(1, height / ROWS)
        return (sigma / kotlin.math.sqrt(pixels.toDouble())).toFloat()
    }

    fun of(luma: IntArray, width: Int, height: Int): ByteArray? {
        if (width < 1 || height < 1 || luma.size < width * height) return null
        val out = ByteArray(SIZE)
        for (row in 0 until ROWS) {
            val y0 = row * height / ROWS
            val y1 = maxOf(y0 + 1, (row + 1) * height / ROWS).coerceAtMost(height)
            for (col in 0 until COLS) {
                val x0 = col * width / COLS
                val x1 = maxOf(x0 + 1, (col + 1) * width / COLS).coerceAtMost(width)
                var sum = 0L
                for (y in y0 until y1) for (x in x0 until x1) sum += luma[y * width + x]
                out[row * COLS + col] = (sum / ((x1 - x0) * (y1 - y0))).toInt().coerceIn(0, 255).toByte()
            }
        }
        return out
    }

    fun gap(a: ByteArray, b: ByteArray, changedAbove: Double = CELL_CHANGED): Gap {
        if (a.size != SIZE || b.size != SIZE) return Gap(255.0, 255.0, 1.0)
        var sa = 0L
        var sb = 0L
        for (i in 0 until SIZE) {
            sa += a[i].toInt() and 0xFF
            sb += b[i].toInt() and 0xFF
        }
        val shift = (sa - sb).toDouble() / SIZE
        var total = 0.0
        var worst = 0.0
        var changed = 0
        for (i in 0 until SIZE) {
            val d = abs((a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF) - shift)
            total += d
            if (d > worst) worst = d
            if (d > changedAbove) changed++
        }
        return Gap(total / SIZE, worst, changed.toDouble() / SIZE)
    }

    fun same(a: ByteArray, b: ByteArray, tolerance: Double = 1.0, noiseA: Float = 0f, noiseB: Float = 0f): Boolean {
        val n = kotlin.math.hypot(noiseA.toDouble(), noiseB.toDouble())
        val g = gap(a, b, (CELL_CHANGED + 3.0 * n) * tolerance)
        return g.meanDiff <= (MAX_MEAN_DIFF + 1.2 * n) * tolerance &&
            g.maxDiff <= (MAX_CELL_DIFF + 4.0 * n) * tolerance &&
            g.changedShare <= MAX_CHANGED_SHARE * tolerance
    }
}
