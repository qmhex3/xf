package com.frameout.engine.decode

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.media.MediaFormat
import com.frameout.core.hdr.HdrSignal
import java.nio.ByteBuffer

class YuvBitmapConverter {
    var hdrGainmap = false
    var captureSignal = false
    var variantId = ColorVariants.AUTO
    private var signal: HdrSignal? = null
    var rotation = 0
    var flipH = false
    var aspect = 1f
    var oriented = false
        private set
    private var pixelBuffer = IntArray(0)
    private val planeData = Array(3) { ByteArray(0) }
    private val kernel = Yuv8Kernel()
    private val rangeProbe = RangeProbe()

    private var colorLogged = false

    fun resetRange() {
        rangeProbe.reset()
        colorLogged = false
    }

    fun takeSignal(): HdrSignal? = signal.also { signal = null }

    private companion object {
        const val PAD = 4
        const val RETAIN_LIMIT_BYTES = 96L * 1024L * 1024L
    }

    internal fun peekParams(image: Image, format: MediaFormat?, fallback: MediaFormat?): ColorParams =
        rangeResolved(ColorParams.from(format, fallback), image)

    fun convert(image: Image, format: MediaFormat? = null, fallback: MediaFormat? = null): Bitmap? {
        val params = ColorVariants.apply(variantId, peekParams(image, format, fallback))
        if (!colorLogged) {
            colorLogged = true
            logColor(params, format, fallback, image.format)
        }
        oriented = false
        signal = null
        return try {
            when (image.format) {
                ImageFormat.YUV_420_888 -> convert8(image, params)
                54 -> convert10(image, params)
                else -> null
            }
        } finally {
            trim()
        }
    }

    private fun logColor(p: ColorParams, out: MediaFormat?, input: MediaFormat?, imageFormat: Int) {
        fun keys(f: MediaFormat?): String = if (f == null) "-" else listOf(
            MediaFormat.KEY_COLOR_RANGE, MediaFormat.KEY_COLOR_STANDARD, MediaFormat.KEY_COLOR_TRANSFER, ColorHints.KEY_PRIMARIES, ColorHints.KEY_MATRIX
        ).joinToString(",") { k -> if (f.containsKey(k)) f.getInteger(k).toString() else "_" } +
            " " + (if (f.containsKey(MediaFormat.KEY_MIME)) f.getString(MediaFormat.KEY_MIME) else "?") +
            " " + (if (f.containsKey(MediaFormat.KEY_WIDTH)) "${f.getInteger(MediaFormat.KEY_WIDTH)}x${f.getInteger(MediaFormat.KEY_HEIGHT)}" else "?")
        com.frameout.core.util.DiagLog.append(
            "ColorParams",
            "limited=${p.limited} declared=${p.rangeDeclared} bt601=${p.bt601} bt2020=${p.bt2020} transfer=${p.transfer} primaries=${p.primaries} matrix=${p.matrix} gamut=${p.gamutConvert} img=$imageFormat out[range,std,trf,prim,mat]=${keys(out)} in=${keys(input)}"
        )
    }

    private fun rangeResolved(params: ColorParams, image: Image): ColorParams {
        if (params.rangeDeclared) return params
        val wide = image.format == 54
        val plane = image.planes[0]
        val buffer = plane.buffer
        val crop = image.cropRect
        return rangeProbe.resolve(params, wide, crop.width(), crop.height()) { x, y ->
            val pos = (crop.top + y) * plane.rowStride + (crop.left + x) * plane.pixelStride
            when {
                pos < 0 || pos >= buffer.limit() -> 128
                wide -> if (pos + 1 < buffer.limit()) (((buffer.get(pos + 1).toInt() and 255) shl 8) or (buffer.get(pos).toInt() and 255)) ushr 6 else 512
                else -> buffer.get(pos).toInt() and 255
            }
        }
    }

    fun release() {
        pixelBuffer = IntArray(0)
        for (slot in planeData.indices) planeData[slot] = ByteArray(0)
        kernel.release()
    }

    private fun trim() {
        val held = pixelBuffer.size * 4L + planeData.sumOf { it.size.toLong() }
        if (held > RETAIN_LIMIT_BYTES) release()
    }

    private fun convert8(image: Image, c: ColorParams): Bitmap {
        val p = image.planes
        val crop = image.cropRect
        val (step, width, height) = scaleFor(crop)
        val pixels = ensurePixels(width * height)
        val src = Yuv8Planes(
            read(p[0].buffer, 0), read(p[1].buffer, 1), read(p[2].buffer, 2),
            p[0].rowStride, p[0].pixelStride, p[1].rowStride, p[1].pixelStride, p[2].rowStride, p[2].pixelStride,
            crop, (image.width + 1) / 2, (image.height + 1) / 2
        )
        val orient = orientFor(width, height, true)
        kernel.convert(src, c, step, width, height, pixels, orient)
        return finished(orient, pixels, true)
    }

    private fun convert10(image: Image, c: ColorParams): Bitmap {
        val yPlane = image.planes[0]
        val uv = image.planes[1]
        val crop = image.cropRect
        val (step, width, height) = scaleFor(crop)
        val pixels = ensurePixels(width * height)
        val mapper = if (c.hdr) HdrToneMapper(c) else null
        val gains = if (mapper != null && hdrGainmap) GainmapBuilder(width, height) else null
        val orient = orientFor(width, height, gains == null)
        val yData = read(yPlane.buffer, 0)
        val uvData = read(uv.buffer, 1)
        val chromaWidth = (image.width + 1) / 2
        val chromaHeight = (image.height + 1) / 2
        val exact = captureSignal && c.hdr && step == 1 && aspect == 1f
        val signalOrient = if (exact) Orient.of(rotation, flipH, width, height) else null
        val rgb16 = if (signalOrient != null) ShortArray(width * height * 3) else null
        fun job(m: HdrToneMapper?) = Yuv10Job(
            yData, uvData, yPlane.rowStride, yPlane.pixelStride, uv.rowStride, uv.pixelStride,
            crop, chromaWidth, chromaHeight, step, width, c, m, gains, orient, rgb16, signalOrient
        )
        YuvPool.run(YuvPool.bands(width, height), height) { first, last ->
            job(if (c.hdr) HdrToneMapper(c, trackGain = gains != null) else null).render(first, last, pixels)
        }
        if (rgb16 != null && signalOrient != null) signal = HdrSignal(signalOrient.outWidth, signalOrient.outHeight, rgb16, SignalCodes.primaries(c), SignalCodes.transfer(c))
        return finished(orient, pixels, gains == null).also { gains?.attach(it) }
    }

    private fun orientFor(width: Int, height: Int, allowed: Boolean): Orient =
        if (allowed && aspect == 1f && (FrameOrientation.normalize(rotation) != 0 || flipH)) Orient.of(rotation, flipH, width, height)
        else Orient.of(0, false, width, height)

    private fun finished(orient: Orient, pixels: IntArray, allowed: Boolean): Bitmap {
        oriented = allowed && aspect == 1f && (FrameOrientation.normalize(rotation) != 0 || flipH)
        return bitmap(orient.outWidth, orient.outHeight, pixels)
    }

    private data class Scale(val step: Int, val width: Int, val height: Int)

    private fun scaleFor(crop: android.graphics.Rect): Scale {
        val step = DecodeBudget.step(crop.width(), crop.height())
        val scale = Scale(step, DecodeBudget.scaled(crop.width(), step), DecodeBudget.scaled(crop.height(), step))
        if (step > 1) DecodeQuality.markReduced("decoder frame ${crop.width()}x${crop.height()} scaled to ${scale.width}x${scale.height} to fit the memory budget", crop.width(), crop.height(), scale.width, scale.height)
        return scale
    }

    private fun read(buffer: ByteBuffer, slot: Int): ByteArray {
        val view = buffer.duplicate()
        view.rewind()
        val size = view.remaining()
        if (planeData[slot].size < size + PAD) planeData[slot] = ByteArray(size + PAD)
        view.get(planeData[slot], 0, size)
        planeData[slot].fill(0, size, size + PAD)
        return planeData[slot]
    }

    private fun ensurePixels(size: Int): IntArray {
        if (pixelBuffer.size < size) pixelBuffer = IntArray(size)
        return pixelBuffer
    }

    private fun bitmap(width: Int, height: Int, pixels: IntArray): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { setPixels(pixels, 0, width, 0, 0, width, height) }
}
