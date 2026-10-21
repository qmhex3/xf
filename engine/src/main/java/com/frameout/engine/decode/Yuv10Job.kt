package com.frameout.engine.decode

import android.graphics.Rect

internal class Yuv10Job(
    private val yData: ByteArray,
    private val uvData: ByteArray,
    private val yRowStride: Int,
    private val yPixelStride: Int,
    private val uvRowStride: Int,
    private val uvPixelStride: Int,
    private val crop: Rect,
    chromaWidth: Int,
    chromaHeight: Int,
    private val step: Int,
    private val width: Int,
    private val params: ColorParams,
    private val mapper: HdrToneMapper?,
    private val gains: GainmapBuilder?,
    private val orient: Orient,
    private val signal: ShortArray? = null,
    private val signalOrient: Orient? = null
) {
    private val sampler = ChromaSampler(chromaWidth, chromaHeight)
    private val tmp = FloatArray(3)
    private val rgb = FloatArray(3)
    private val special = params.special
    private val taps = DecodeBudget.taps(step)
    private val inv = 1f / (taps * taps)
    private val cScale = if (params.limited) 1f / 896f else 1f / 1023f
    private val rv = params.rv
    private val gu = params.gu
    private val gv = params.gv
    private val bu = params.bu

    fun render(first: Int, last: Int, pixels: IntArray) {
        if (taps == 1) {
            renderSingle(first, last, pixels)
            return
        }
        for (row in first until last) {
            for (col in 0 until width) {
                pixels[orient.index(col, row)] = pixel(row, col)
                if (mapper != null) gains?.add(row * width + col, mapper.gainCodes)
            }
        }
    }

    private fun renderSingle(first: Int, last: Int, pixels: IntArray) {
        val limited = params.limited
        for (row in first until last) {
            val sr = DecodeBudget.sourceIndex(row, 1, 0, 1, crop.height()) + crop.top
            sampler.row(sr)
            val yRow = sr * yRowStride
            val c0 = sampler.y0 * uvRowStride
            val c1 = sampler.y1 * uvRowStride
            for (col in 0 until width) {
                val sc = DecodeBudget.sourceIndex(col, 1, 0, 1, crop.width()) + crop.left
                sampler.col(sc)
                val y = sample(yData, yRow + sc * yPixelStride)
                val yy = if (limited) (y - 64) / 876f else y / 1023f
                val uu = sampler.lerp10(uvData, c0, c1, uvPixelStride, 0, cScale)
                val vv = sampler.lerp10(uvData, c0, c1, uvPixelStride, 2, cScale)
                pixels[orient.index(col, row)] = if (special != 0) {
                    SpecialMatrix.convert(params, yy, uu, vv, rgb)
                    finish(rgb[0], rgb[1], rgb[2], col, row)
                } else {
                    finish(yy + rv * vv, yy - gu * uu - gv * vv, yy + bu * uu, col, row)
                }
                if (mapper != null) gains?.add(row * width + col, mapper.gainCodes)
            }
        }
    }

    private fun pixel(row: Int, col: Int): Int {
        var r = 0f
        var g = 0f
        var b = 0f
        for (ty in 0 until taps) {
            val sr = DecodeBudget.sourceIndex(row, step, ty, taps, crop.height()) + crop.top
            sampler.row(sr)
            val yRow = sr * yRowStride
            val c0 = sampler.y0 * uvRowStride
            val c1 = sampler.y1 * uvRowStride
            for (tx in 0 until taps) {
                val sc = DecodeBudget.sourceIndex(col, step, tx, taps, crop.width()) + crop.left
                sampler.col(sc)
                val y = sample(yData, yRow + sc * yPixelStride)
                val yy = if (params.limited) (y - 64) / 876f else y / 1023f
                val uu = sampler.lerp10(uvData, c0, c1, uvPixelStride, 0, cScale)
                val vv = sampler.lerp10(uvData, c0, c1, uvPixelStride, 2, cScale)
                if (special != 0) {
                    SpecialMatrix.convert(params, yy, uu, vv, rgb)
                    r += rgb[0]; g += rgb[1]; b += rgb[2]
                } else {
                    r += yy + rv * vv
                    g += yy - gu * uu - gv * vv
                    b += yy + bu * uu
                }
            }
        }
        return finish(r * inv, g * inv, b * inv, col, row)
    }

    private fun finish(r: Float, g: Float, b: Float, x: Int, y: Int): Int {
        if (signal != null && signalOrient != null) store(signal, signalOrient.index(x, y) * 3, r, g, b)
        if (mapper != null) return mapper.map(r, g, b, x, y)
        val t = Dither.threshold(x, y)
        if (!params.gamutConvert) return Dither.pack(r, g, b, t)
        GamutConvert.convert(params.gamutMatrix, r, g, b, tmp)
        return Dither.pack(tmp[0], tmp[1], tmp[2], t)
    }

    private fun store(target: ShortArray, at: Int, r: Float, g: Float, b: Float) {
        target[at] = code16(r)
        target[at + 1] = code16(g)
        target[at + 2] = code16(b)
    }

    private fun code16(v: Float): Short = (v.coerceIn(0f, 1f) * 65535f + 0.5f).toInt().toShort()

    private fun sample(data: ByteArray, offset: Int): Int =
        if (offset < 0 || offset + 1 >= data.size) 0 else (((data[offset + 1].toInt() and 255) shl 8) or (data[offset].toInt() and 255)) ushr 6
}
