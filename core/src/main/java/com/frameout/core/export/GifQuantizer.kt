package com.frameout.core.export

object GifQuantizer {
    private const val RED = 6
    private const val GREEN = 7
    private const val BLUE = 6
    private val bayer = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

    val palette: ByteArray = ByteArray(256 * 3).also { table ->
        for (r in 0 until RED) for (g in 0 until GREEN) for (b in 0 until BLUE) {
            val at = ((r * GREEN + g) * BLUE + b) * 3
            table[at] = level(r, RED).toByte()
            table[at + 1] = level(g, GREEN).toByte()
            table[at + 2] = level(b, BLUE).toByte()
        }
    }

    fun indices(argb: IntArray, width: Int, height: Int): ByteArray {
        val result = ByteArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            val pixel = argb[y * width + x]
            val shift = (bayer[(y and 3) * 4 + (x and 3)] - 7.5f) / 16f
            val r = snap((pixel shr 16) and 0xFF, RED, shift)
            val g = snap((pixel shr 8) and 0xFF, GREEN, shift)
            val b = snap(pixel and 0xFF, BLUE, shift)
            result[y * width + x] = ((r * GREEN + g) * BLUE + b).toByte()
        }
        return result
    }

    private fun level(index: Int, levels: Int): Int = (index * 255f / (levels - 1)).toInt()

    private fun snap(value: Int, levels: Int, shift: Float): Int {
        val scaled = value / (255f / (levels - 1)) + shift
        return (scaled + 0.5f).toInt().coerceIn(0, levels - 1)
    }
}
