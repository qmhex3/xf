package com.frameout.core.quality

import android.graphics.Bitmap
import kotlin.math.abs

object SlideSignature {
    const val COLS = 32
    const val ROWS = 18
    private const val SUB = 3
    private const val CELL_DELTA = 0.10f
    private const val CELL_RATIO = 0.045f
    private const val MEAN_DELTA = 0.04f

    fun of(bitmap: Bitmap): FloatArray = of(bitmap.width, bitmap.height) { x, y ->
        val p = bitmap.getPixel(x, y)
        (77 * ((p shr 16) and 0xFF) + 150 * ((p shr 8) and 0xFF) + 29 * (p and 0xFF)) shr 8
    }

    fun of(w: Int, h: Int, luma: LumaGrid): FloatArray {
        val out = FloatArray(COLS * ROWS)
        if (w <= 0 || h <= 0) return out
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                var sum = 0
                for (sy in 0 until SUB) {
                    val y = (((row * SUB + sy) * 2 + 1) * h / (ROWS * SUB * 2)).coerceIn(0, h - 1)
                    for (sx in 0 until SUB) {
                        val x = (((col * SUB + sx) * 2 + 1) * w / (COLS * SUB * 2)).coerceIn(0, w - 1)
                        sum += luma.at(x, y)
                    }
                }
                out[row * COLS + col] = sum / (SUB * SUB * 255f)
            }
        }
        return out
    }

    fun differs(a: FloatArray, b: FloatArray): Boolean {
        val n = minOf(a.size, b.size)
        if (n == 0) return true
        var changed = 0
        var total = 0f
        for (i in 0 until n) {
            val d = abs(a[i] - b[i])
            total += d
            if (d > CELL_DELTA) changed++
        }
        return changed.toFloat() / n > CELL_RATIO || total / n > MEAN_DELTA
    }
}
