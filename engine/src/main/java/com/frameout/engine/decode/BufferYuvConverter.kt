package com.frameout.engine.decode

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaFormat
import java.nio.ByteBuffer

internal class BufferYuvConverter {
    private var scratch = ByteArray(0)
    private var pixelBuffer = IntArray(0)
    private val kernel = Yuv8Kernel()
    private val rangeProbe = RangeProbe()

    var variantId = ColorVariants.AUTO

    fun resetRange() = rangeProbe.reset()

    fun convert(buffer: ByteBuffer, format: MediaFormat, fallback: MediaFormat?): Bitmap? {
        val color = format.getInteger(MediaFormat.KEY_COLOR_FORMAT)
        val semi = color == SEMI_PLANAR || color == PACKED_SEMI_PLANAR || color == P010
        if (!semi && color != PLANAR && color != PACKED_PLANAR) return null
        val layout = Layout.from(format, semi, if (color == P010) 2 else 1)
        val data = load(buffer)
        val params = ColorVariants.apply(variantId, rangeResolved(ColorParams.from(format, fallback), data, layout, color == P010))
        try {
            return render(data, layout, params, color)
        } finally {
            trim()
        }
    }

    private fun rangeResolved(params: ColorParams, data: ByteArray, l: Layout, wide: Boolean): ColorParams {
        if (params.rangeDeclared) return params
        val bytes = if (wide) 2 else 1
        return rangeProbe.resolve(params, wide, l.width, l.height) { x, y ->
            val pos = (l.top + y) * l.stride + (l.left + x) * bytes
            when {
                pos < 0 || pos + bytes > data.size -> 128
                wide -> (((data[pos + 1].toInt() and 255) shl 8) or (data[pos].toInt() and 255)) ushr 6
                else -> data[pos].toInt() and 255
            }
        }
    }

    fun release() {
        scratch = ByteArray(0)
        pixelBuffer = IntArray(0)
        kernel.release()
    }

    private fun trim() {
        if (scratch.size + pixelBuffer.size * 4L > RETAIN_LIMIT_BYTES) release()
    }

    private fun render(data: ByteArray, layout: Layout, params: ColorParams, color: Int): Bitmap {
        val step = DecodeBudget.step(layout.width, layout.height)
        val outW = DecodeBudget.scaled(layout.width, step)
        val outH = DecodeBudget.scaled(layout.height, step)
        if (step > 1) DecodeQuality.markReduced("decoder frame ${layout.width}x${layout.height} scaled to ${outW}x${outH} to fit the memory budget", layout.width, layout.height, outW, outH)
        val pixels = ensurePixels(outW * outH)
        if (color == P010) wide(data, layout, params, pixels, step, outW, outH) else narrow(data, layout, params, pixels, step, outW, outH)
        return Bitmap.createBitmap(pixels, 0, outW, outW, outH, Bitmap.Config.ARGB_8888)
    }

    private fun load(buffer: ByteBuffer): ByteArray {
        val view = buffer.duplicate()
        view.rewind()
        val size = view.remaining()
        if (scratch.size < size + PAD) scratch = ByteArray(size + PAD)
        view.get(scratch, 0, size)
        scratch.fill(0, size, size + PAD)
        return scratch
    }

    private fun ensurePixels(size: Int): IntArray {
        if (pixelBuffer.size < size) pixelBuffer = IntArray(size)
        return pixelBuffer
    }

    private fun narrow(data: ByteArray, l: Layout, c: ColorParams, pixels: IntArray, step: Int, outW: Int, outH: Int) {
        val chromaBase = l.stride * l.slice
        val half = l.stride / 2
        val vShift = if (l.semi) 1 else half * (l.slice / 2)
        val rowStride = if (l.semi) l.stride else half
        val pixelStride = if (l.semi) 2 else 1
        val crop = Rect(l.left, l.top, l.left + l.width, l.top + l.height)
        val src = Yuv8Planes(
            data, data, data, l.stride, 1, rowStride, pixelStride, rowStride, pixelStride,
            crop, (l.left + l.width + 1) / 2, (l.top + l.height + 1) / 2, chromaBase, chromaBase + vShift
        )
        kernel.convert(src, c, step, outW, outH, pixels, Orient.of(0, false, outW, outH))
    }

    private fun wide(data: ByteArray, l: Layout, c: ColorParams, pixels: IntArray, step: Int, outW: Int, outH: Int) {
        val mapper = if (c.hdr) HdrToneMapper(c) else null
        val chromaBase = l.stride * l.slice
        val gamut = c.gamutConvert
        val tmp = FloatArray(3)
        val rgb = FloatArray(3)
        val cs = ChromaSampler((l.left + l.width + 1) / 2, (l.top + l.height + 1) / 2)
        val cScale = if (c.limited) 1f / 896f else 1f / 1023f
        val taps = DecodeBudget.taps(step)
        val inv = 1f / (taps * taps)
        for (row in 0 until outH) {
            val out = row * outW
            for (col in 0 until outW) {
                var r = 0f; var g = 0f; var b = 0f
                for (ty in 0 until taps) {
                    val sr = DecodeBudget.sourceIndex(row, step, ty, taps, l.height) + l.top
                    cs.row(sr)
                    val yRow = sr * l.stride
                    val c0 = chromaBase + cs.y0 * l.stride
                    val c1 = chromaBase + cs.y1 * l.stride
                    for (tx in 0 until taps) {
                        val sc = DecodeBudget.sourceIndex(col, step, tx, taps, l.width) + l.left
                        cs.col(sc)
                        val y = sample(data, yRow + sc * 2)
                        val yy = if (c.limited) (y - 64) / 876f else y / 1023f
                        val uu = cs.lerp10(data, c0, c1, 4, 0, cScale)
                        val vv = cs.lerp10(data, c0, c1, 4, 2, cScale)
                        toRgb(c, yy, uu, vv, rgb)
                        r += rgb[0]; g += rgb[1]; b += rgb[2]
                    }
                }
                r *= inv; g *= inv; b *= inv
                pixels[out + col] = if (mapper != null) mapper.map(r, g, b, col, row) else plain(r, g, b, gamut, c.gamutMatrix, tmp, col, row)
            }
        }
    }

    private fun toRgb(c: ColorParams, yy: Float, uu: Float, vv: Float, rgb: FloatArray) {
        if (c.special != 0) {
            SpecialMatrix.convert(c, yy, uu, vv, rgb)
            return
        }
        rgb[0] = yy + c.rv * vv
        rgb[1] = yy - c.gu * uu - c.gv * vv
        rgb[2] = yy + c.bu * uu
    }

    private fun plain(r: Float, g: Float, b: Float, gamut: Boolean, matrix: FloatArray, tmp: FloatArray, x: Int, y: Int): Int {
        val t = Dither.threshold(x, y)
        if (!gamut) return Dither.pack(r, g, b, t)
        GamutConvert.convert(matrix, r, g, b, tmp)
        return Dither.pack(tmp[0], tmp[1], tmp[2], t)
    }

    private fun sample(data: ByteArray, offset: Int): Int =
        if (offset < 0 || offset + 1 >= data.size) 0 else (((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset].toInt() and 0xFF)) ushr 6


    private class Layout(val width: Int, val height: Int, val left: Int, val top: Int, val stride: Int, val slice: Int, val semi: Boolean) {
        companion object {
            fun from(format: MediaFormat, semi: Boolean, bytesPerSample: Int): Layout {
                val width = format.getInteger(MediaFormat.KEY_WIDTH)
                val height = format.getInteger(MediaFormat.KEY_HEIGHT)
                val left = int(format, "crop-left", 0)
                val top = int(format, "crop-top", 0)
                val right = int(format, "crop-right", width - 1)
                val bottom = int(format, "crop-bottom", height - 1)
                return Layout(right - left + 1, bottom - top + 1, left, top, int(format, "stride", width * bytesPerSample), int(format, "slice-height", height), semi)
            }

            private fun int(format: MediaFormat, key: String, fallback: Int): Int = if (format.containsKey(key)) format.getInteger(key) else fallback
        }
    }

    private companion object {
        const val PLANAR = 19
        const val PACKED_PLANAR = 20
        const val SEMI_PLANAR = 21
        const val PACKED_SEMI_PLANAR = 39
        const val P010 = 54
        const val PAD = 4
        const val RETAIN_LIMIT_BYTES = 96L * 1024L * 1024L
    }
}
