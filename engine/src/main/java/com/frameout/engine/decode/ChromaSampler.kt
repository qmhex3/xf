package com.frameout.engine.decode

internal class ChromaSampler(private val width: Int, private val height: Int) {
    var y0 = 0
        private set
    var y1 = 0
        private set
    var x0 = 0
        private set
    var x1 = 0
        private set
    var fy = 0f
        private set
    var fx = 0f
        private set

    fun row(luma: Int) {
        val k = (luma - 1) shr 1
        y0 = k.coerceIn(0, height - 1)
        y1 = (k + 1).coerceIn(0, height - 1)
        fy = if (luma and 1 == 1) 0.25f else 0.75f
    }

    fun col(luma: Int) {
        x0 = (luma shr 1).coerceAtMost(width - 1)
        val odd = luma and 1 == 1
        x1 = if (odd) (x0 + 1).coerceAtMost(width - 1) else x0
        fx = if (odd) 0.5f else 0f
    }

    fun lerp(table: FloatArray, data: ByteArray, r0: Int, r1: Int, pixelStride: Int): Float {
        val a = table[data[r0 + x0 * pixelStride].toInt() and 255]
        val b = table[data[r0 + x1 * pixelStride].toInt() and 255]
        val c = table[data[r1 + x0 * pixelStride].toInt() and 255]
        val d = table[data[r1 + x1 * pixelStride].toInt() and 255]
        return mix(a, b, c, d)
    }

    fun lerp10(data: ByteArray, r0: Int, r1: Int, pixelStride: Int, extra: Int, scale: Float): Float {
        val a = raw10(data, r0 + x0 * pixelStride + extra)
        val b = raw10(data, r0 + x1 * pixelStride + extra)
        val c = raw10(data, r1 + x0 * pixelStride + extra)
        val d = raw10(data, r1 + x1 * pixelStride + extra)
        return (mix(a, b, c, d) - 512f) * scale
    }

    private fun mix(a: Float, b: Float, c: Float, d: Float): Float {
        val top = a + (b - a) * fx
        val bottom = c + (d - c) * fx
        return top + (bottom - top) * fy
    }

    private fun raw10(data: ByteArray, offset: Int): Float {
        if (offset < 0 || offset + 1 >= data.size) return 512f
        return ((((data[offset + 1].toInt() and 255) shl 8) or (data[offset].toInt() and 255)) ushr 6).toFloat()
    }
}
