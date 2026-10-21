package com.frameout.core.storage

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import java.io.OutputStream

internal object Jpeg444 {
    fun supports(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled || bitmap.config != Bitmap.Config.ARGB_8888) return false
        if (bitmap.width !in 1..65535 || bitmap.height !in 1..65535) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val space = bitmap.colorSpace ?: return true
        return space == ColorSpace.get(ColorSpace.Named.SRGB)
    }

    fun encode(bitmap: Bitmap, quality: Int, out: OutputStream): Boolean {
        if (!supports(bitmap)) return false
        val tables = Tables(quality.coerceIn(1, 100))
        writeHeaders(out, bitmap.width, bitmap.height, tables.lumaQ, tables.chromaQ)
        val sink = BitSink(out)
        encodeBands(bitmap, sink, tables)
        sink.finish()
        out.write(0xFF)
        out.write(0xD9)
        out.flush()
        return true
    }

    private fun encodeBands(bitmap: Bitmap, sink: BitSink, tables: Tables) {
        val width = bitmap.width
        val height = bitmap.height
        val band = IntArray(width * 8)
        val planes = Array(3) { FloatArray(64) }
        val coefficients = IntArray(64)
        val predictors = IntArray(3)
        var top = 0
        while (top < height) {
            val rows = minOf(8, height - top)
            bitmap.getPixels(band, 0, width, 0, top, width, rows)
            var left = 0
            while (left < width) {
                val cols = minOf(8, width - left)
                fill(band, width, left, rows, cols, planes)
                for (component in 0 until 3) {
                    transform(planes[component], tables.divisor(component), coefficients)
                    emit(sink, coefficients, predictors, component, tables.dc(component), tables.ac(component))
                }
                left += 8
            }
            top += 8
        }
    }

    private class Tables(quality: Int) {
        private val scale = if (quality < 50) 5000 / quality else 200 - 2 * quality
        val lumaQ = scaled(LUMA_Q, scale)
        val chromaQ = scaled(CHROMA_Q, scale)
        private val lumaDiv = divisors(lumaQ)
        private val chromaDiv = divisors(chromaQ)
        private val dcLuma = Huffman(DC_LUMA_BITS, DC_VALS)
        private val dcChroma = Huffman(DC_CHROMA_BITS, DC_VALS)
        private val acLuma = Huffman(AC_LUMA_BITS, AC_LUMA_VALS)
        private val acChroma = Huffman(AC_CHROMA_BITS, AC_CHROMA_VALS)
        fun divisor(component: Int) = if (component == 0) lumaDiv else chromaDiv
        fun dc(component: Int) = if (component == 0) dcLuma else dcChroma
        fun ac(component: Int) = if (component == 0) acLuma else acChroma
    }

    private fun fill(band: IntArray, width: Int, left: Int, rows: Int, cols: Int, planes: Array<FloatArray>) {
        val yPlane = planes[0]
        val cbPlane = planes[1]
        val crPlane = planes[2]
        for (r in 0 until 8) {
            val rowBase = (if (r < rows) r else rows - 1) * width + left
            for (c in 0 until 8) {
                val p = band[rowBase + (if (c < cols) c else cols - 1)]
                val alpha = p ushr 24
                val red = onWhite((p shr 16) and 255, alpha)
                val green = onWhite((p shr 8) and 255, alpha)
                val blue = onWhite(p and 255, alpha)
                val i = r * 8 + c
                yPlane[i] = 0.299f * red + 0.587f * green + 0.114f * blue - 128f
                cbPlane[i] = -0.168736f * red - 0.331264f * green + 0.5f * blue
                crPlane[i] = 0.5f * red - 0.418688f * green - 0.081312f * blue
            }
        }
    }

    private fun onWhite(channel: Int, alpha: Int): Float =
        if (alpha == 255) channel.toFloat() else (channel * alpha + 255 * (255 - alpha)) / 255f

    private fun transform(d: FloatArray, divisor: FloatArray, out: IntArray) {
        for (row in 0 until 8) dct(d, row * 8, 1)
        for (col in 0 until 8) dct(d, col, 8)
        for (i in 0 until 64) {
            val v = d[i] * divisor[i]
            out[i] = (if (v >= 0f) v + 0.5f else v - 0.5f).toInt()
        }
    }

    private fun dct(d: FloatArray, start: Int, step: Int) {
        val d0 = d[start]
        val d1 = d[start + step]
        val d2 = d[start + 2 * step]
        val d3 = d[start + 3 * step]
        val d4 = d[start + 4 * step]
        val d5 = d[start + 5 * step]
        val d6 = d[start + 6 * step]
        val d7 = d[start + 7 * step]
        val t0 = d0 + d7
        val t7 = d0 - d7
        val t1 = d1 + d6
        val t6 = d1 - d6
        val t2 = d2 + d5
        val t5 = d2 - d5
        val t3 = d3 + d4
        val t4 = d3 - d4
        val e0 = t0 + t3
        val e3 = t0 - t3
        val e1 = t1 + t2
        val e2 = t1 - t2
        d[start] = e0 + e1
        d[start + 4 * step] = e0 - e1
        val z1 = (e2 + e3) * 0.707106781f
        d[start + 2 * step] = e3 + z1
        d[start + 6 * step] = e3 - z1
        val f0 = t4 + t5
        val f1 = t5 + t6
        val f2 = t6 + t7
        val z5 = (f0 - f2) * 0.382683433f
        val z2 = 0.541196100f * f0 + z5
        val z4 = 1.306562965f * f2 + z5
        val z3 = f1 * 0.707106781f
        val z11 = t7 + z3
        val z13 = t7 - z3
        d[start + 5 * step] = z13 + z2
        d[start + 3 * step] = z13 - z2
        d[start + step] = z11 + z4
        d[start + 7 * step] = z11 - z4
    }

    private fun emit(sink: BitSink, z: IntArray, predictors: IntArray, component: Int, dc: Huffman, ac: Huffman) {
        val diff = z[0] - predictors[component]
        predictors[component] = z[0]
        val dcSize = magnitude(diff)
        sink.put(dc.code[dcSize], dc.size[dcSize])
        if (dcSize > 0) sink.put(if (diff > 0) diff else diff - 1, dcSize)
        var run = 0
        for (k in 1 until 64) {
            val v = z[ZIGZAG[k]]
            if (v == 0) {
                run++
                continue
            }
            while (run > 15) {
                sink.put(ac.code[0xF0], ac.size[0xF0])
                run -= 16
            }
            val s = magnitude(v)
            val symbol = (run shl 4) or s
            sink.put(ac.code[symbol], ac.size[symbol])
            sink.put(if (v > 0) v else v - 1, s)
            run = 0
        }
        if (run > 0) sink.put(ac.code[0], ac.size[0])
    }

    private fun magnitude(v: Int): Int = 32 - Integer.numberOfLeadingZeros(if (v < 0) -v else v)

    private fun scaled(base: IntArray, scale: Int): IntArray =
        IntArray(64) { ((base[it] * scale + 50) / 100).coerceIn(1, 255) }

    private fun divisors(table: IntArray): FloatArray = FloatArray(64) {
        val row = it / 8
        val col = it % 8
        1f / (table[it] * AAN[row] * AAN[col] * 8f)
    }

    private val AAN = floatArrayOf(1.0f, 1.387039845f, 1.306562965f, 1.175875602f, 1.0f, 0.785694958f, 0.541196100f, 0.275899379f)

    private fun writeHeaders(out: OutputStream, width: Int, height: Int, lumaQ: IntArray, chromaQ: IntArray) {
        out.write(0xFF)
        out.write(0xD8)
        segment(out, 0xE0, byteArrayOf(0x4A, 0x46, 0x49, 0x46, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0))
        segment(out, 0xDB, ByteArray(65).also { b -> b[0] = 0; for (k in 0 until 64) b[k + 1] = lumaQ[ZIGZAG[k]].toByte() })
        segment(out, 0xDB, ByteArray(65).also { b -> b[0] = 1; for (k in 0 until 64) b[k + 1] = chromaQ[ZIGZAG[k]].toByte() })
        segment(
            out, 0xC0,
            byteArrayOf(
                8, (height shr 8).toByte(), height.toByte(), (width shr 8).toByte(), width.toByte(),
                3, 1, 0x11, 0, 2, 0x11, 1, 3, 0x11, 1
            )
        )
        table(out, 0x00, DC_LUMA_BITS, DC_VALS)
        table(out, 0x10, AC_LUMA_BITS, AC_LUMA_VALS)
        table(out, 0x01, DC_CHROMA_BITS, DC_VALS)
        table(out, 0x11, AC_CHROMA_BITS, AC_CHROMA_VALS)
        segment(out, 0xDA, byteArrayOf(3, 1, 0, 2, 0x11, 3, 0x11, 0, 63, 0))
    }

    private fun table(out: OutputStream, id: Int, bits: IntArray, vals: IntArray) {
        val payload = ByteArray(1 + 16 + vals.size)
        payload[0] = id.toByte()
        for (i in 0 until 16) payload[1 + i] = bits[i].toByte()
        for (i in vals.indices) payload[17 + i] = vals[i].toByte()
        segment(out, 0xC4, payload)
    }

    private fun segment(out: OutputStream, marker: Int, payload: ByteArray) {
        val length = payload.size + 2
        out.write(0xFF)
        out.write(marker)
        out.write(length shr 8)
        out.write(length and 255)
        out.write(payload)
    }

    private class Huffman(bits: IntArray, vals: IntArray) {
        val code = IntArray(256)
        val size = IntArray(256)

        init {
            var next = 0
            var k = 0
            for (length in 1..16) {
                for (i in 0 until bits[length - 1]) {
                    code[vals[k]] = next
                    size[vals[k]] = length
                    next++
                    k++
                }
                next = next shl 1
            }
        }
    }

    private class BitSink(private val out: OutputStream) {
        private val buffer = ByteArray(1 shl 16)
        private var count = 0
        private var accumulator = 0L
        private var bits = 0

        fun put(value: Int, length: Int) {
            if (length == 0) return
            accumulator = (accumulator shl length) or (value.toLong() and ((1L shl length) - 1))
            bits += length
            while (bits >= 8) {
                val b = ((accumulator shr (bits - 8)) and 0xFF).toInt()
                push(b)
                if (b == 0xFF) push(0)
                bits -= 8
            }
            accumulator = accumulator and ((1L shl bits) - 1)
        }

        fun finish() {
            if (bits > 0) put((1 shl (8 - bits)) - 1, 8 - bits)
            out.write(buffer, 0, count)
            count = 0
        }

        private fun push(b: Int) {
            if (count == buffer.size) {
                out.write(buffer, 0, count)
                count = 0
            }
            buffer[count++] = b.toByte()
        }
    }
}
