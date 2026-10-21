package com.frameout.core.export

import android.graphics.Bitmap
import java.io.OutputStream

class GifEncoder(private val out: OutputStream, private val width: Int, private val height: Int, private val delayMs: Int) {
    private var previous: IntArray? = null

    fun begin() {
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeShort(width)
        writeShort(height)
        out.write(0x70)
        out.write(0)
        out.write(0)
        out.write(byteArrayOf(0x21, 0xFF.toByte(), 0x0B))
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(byteArrayOf(0x03, 0x01, 0x00, 0x00, 0x00))
    }

    fun addFrame(bitmap: Bitmap) {
        require(bitmap.width == width && bitmap.height == height)
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
        val shown = previous
        val unchanged = BooleanArray(argb.size)
        val box = changedBox(argb, shown, unchanged)
        val w = box.right - box.left + 1
        val h = box.bottom - box.top + 1
        val palette = GifPalette.build(argb, { i -> !unchanged[i] && inside(i, box.left, box.top, box.right, box.bottom) }, OPAQUE_COLORS)
        val indices = ByteArray(w * h)
        val reconstructed = shown?.copyOf() ?: IntArray(argb.size)
        val hasTransparent = dither(argb, unchanged, box, palette, indices, reconstructed)
        writeGraphicControl(hasTransparent)
        out.write(0x2C)
        writeShort(box.left)
        writeShort(box.top)
        writeShort(w)
        writeShort(h)
        out.write(0x80 or 7)
        out.write(palette.table)
        GifLzwEncoder(out).encode(indices)
        previous = reconstructed
    }

    private class Box(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private fun changedBox(argb: IntArray, shown: IntArray?, unchanged: BooleanArray): Box {
        var left = width; var top = height; var right = -1; var bottom = -1
        for (y in 0 until height) for (x in 0 until width) {
            val i = y * width + x
            if (shown != null && close(argb[i], shown[i])) {
                unchanged[i] = true
            } else {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        if (right < 0) {
            unchanged[0] = true
            return Box(0, 0, 0, 0)
        }
        return Box(left, top, right, bottom)
    }

    private fun dither(argb: IntArray, unchanged: BooleanArray, box: Box, palette: GifPalette, indices: ByteArray, reconstructed: IntArray): Boolean {
        val w = box.right - box.left + 1
        val h = box.bottom - box.top + 1
        var hasTransparent = false
        var errorCurrent = IntArray((w + 2) * 3)
        var errorNext = IntArray((w + 2) * 3)
        for (row in 0 until h) {
            val forward = row % 2 == 0
            java.util.Arrays.fill(errorNext, 0)
            var column = if (forward) 0 else w - 1
            val step = if (forward) 1 else -1
            repeat(w) {
                val source = (box.top + row) * width + box.left + column
                val at = row * w + column
                if (unchanged[source]) {
                    indices[at] = TRANSPARENT.toByte()
                    hasTransparent = true
                } else {
                    val chosen = ditherPixel(argb[source], palette, errorCurrent, errorNext, (column + 1) * 3, step)
                    indices[at] = chosen.toByte()
                    reconstructed[source] = (palette.red(chosen) shl 16) or (palette.green(chosen) shl 8) or palette.blue(chosen)
                }
                column += step
            }
            val swap = errorCurrent
            errorCurrent = errorNext
            errorNext = swap
        }
        return hasTransparent
    }

    private fun ditherPixel(pixel: Int, palette: GifPalette, current: IntArray, next: IntArray, e: Int, step: Int): Int {
        val r = (((pixel shr 16) and 0xFF) + current[e] / 16).coerceIn(0, 255)
        val g = (((pixel shr 8) and 0xFF) + current[e + 1] / 16).coerceIn(0, 255)
        val b = ((pixel and 0xFF) + current[e + 2] / 16).coerceIn(0, 255)
        val chosen = palette.nearest(r, g, b)
        spread(current, next, e, step, r - palette.red(chosen), g - palette.green(chosen), b - palette.blue(chosen))
        return chosen
    }

    fun finish() {
        out.write(0x3B)
        out.flush()
    }

    private fun spread(current: IntArray, next: IntArray, e: Int, step: Int, dr: Int, dg: Int, db: Int) {
        val ahead = e + step * 3
        val behind = e - step * 3
        for (c in 0..2) {
            val error = when (c) { 0 -> dr; 1 -> dg; else -> db }
            if (ahead in current.indices) current[ahead + c] += error * 7
            if (behind in next.indices) next[behind + c] += error * 3
            next[e + c] += error * 5
            if (ahead in next.indices) next[ahead + c] += error
        }
    }

    private fun writeGraphicControl(transparent: Boolean) {
        out.write(byteArrayOf(0x21, 0xF9.toByte(), 0x04, if (transparent) 0x05 else 0x04))
        writeShort((delayMs / 10).coerceAtLeast(2))
        out.write(if (transparent) TRANSPARENT else 0)
        out.write(0)
    }

    private fun inside(i: Int, left: Int, top: Int, right: Int, bottom: Int): Boolean {
        val x = i % width
        val y = i / width
        return x in left..right && y in top..bottom
    }

    private fun close(a: Int, b: Int): Boolean {
        val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
        val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
        val db = (a and 0xFF) - (b and 0xFF)
        return dr * dr + dg * dg + db * db <= TOLERANCE
    }

    private fun writeShort(value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    private companion object {
        const val TRANSPARENT = 255
        const val OPAQUE_COLORS = 255
        const val TOLERANCE = 300
    }
}
