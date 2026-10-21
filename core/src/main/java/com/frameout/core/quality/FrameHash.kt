package com.frameout.core.quality

object FrameHash {
    private const val COLS = 9
    private const val ROWS = 8

    fun dHash(luma: IntArray, width: Int, height: Int): Long {
        if (width < COLS || height < ROWS || luma.size < width * height) return 0L
        val cells = Array(ROWS) { row -> IntArray(COLS) { col -> cellAverage(luma, width, height, COLS, ROWS, col, row) } }
        var hash = 0L
        for (row in 0 until ROWS) {
            for (col in 0 until COLS - 1) {
                hash = (hash shl 1) or if (cells[row][col] > cells[row][col + 1]) 1L else 0L
            }
        }
        return hash
    }

    fun vHash(luma: IntArray, width: Int, height: Int): Long {
        if (width < ROWS || height < COLS || luma.size < width * height) return 0L
        val cells = Array(COLS) { row -> IntArray(ROWS) { col -> cellAverage(luma, width, height, ROWS, COLS, col, row) } }
        var hash = 0L
        for (col in 0 until ROWS) {
            for (row in 0 until COLS - 1) {
                hash = (hash shl 1) or if (cells[row][col] > cells[row + 1][col]) 1L else 0L
            }
        }
        return hash
    }

    fun mean(luma: IntArray, width: Int, height: Int): Int {
        val count = width * height
        if (count <= 0 || luma.size < count) return 0
        var sum = 0L
        for (i in 0 until count) sum += luma[i]
        return (sum / count).toInt()
    }

    fun distance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    private fun cellAverage(luma: IntArray, width: Int, height: Int, cols: Int, rows: Int, col: Int, row: Int): Int {
        val x0 = col * width / cols
        val x1 = maxOf(x0 + 1, (col + 1) * width / cols)
        val y0 = row * height / rows
        val y1 = maxOf(y0 + 1, (row + 1) * height / rows)
        var sum = 0L
        for (y in y0 until y1) for (x in x0 until x1) sum += luma[y * width + x]
        return (sum / ((x1 - x0) * (y1 - y0))).toInt()
    }
}
