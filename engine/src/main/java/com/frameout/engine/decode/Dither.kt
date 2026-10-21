package com.frameout.engine.decode

internal object Dither {
    private val matrix = build()

    fun threshold(x: Int, y: Int): Float = matrix[((y and 7) shl 3) or (x and 7)]

    fun quantize(value: Float, threshold: Float): Int =
        (value.coerceIn(0f, 1f) * 255f + threshold).toInt().coerceAtMost(255)

    fun pack(r: Float, g: Float, b: Float, threshold: Float): Int =
        (0xFF shl 24) or (quantize(r, threshold) shl 16) or (quantize(g, threshold) shl 8) or quantize(b, threshold)

    private fun build(): FloatArray {
        var m = intArrayOf(0)
        var n = 1
        while (n < 8) {
            val s = n * 2
            val next = IntArray(s * s)
            for (y in 0 until n) for (x in 0 until n) {
                val v = m[y * n + x] * 4
                next[y * s + x] = v
                next[y * s + x + n] = v + 2
                next[(y + n) * s + x] = v + 3
                next[(y + n) * s + x + n] = v + 1
            }
            m = next
            n = s
        }
        return FloatArray(64) { (m[it] + 0.5f) / 64f }
    }
}
