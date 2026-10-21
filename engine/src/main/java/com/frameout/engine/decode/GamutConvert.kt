package com.frameout.engine.decode

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal object GamutConvert {
    private const val DECODE_MAX = 1023
    private const val ENCODE_MAX = 4095
    private const val MIN_LUMA = 1e-6f
    private val decodeTable = FloatArray(DECODE_MAX + 1) { srgbDecode(it / DECODE_MAX.toDouble()).toFloat() }
    private val encodeTable = FloatArray(ENCODE_MAX + 1) { srgbEncode(it / ENCODE_MAX.toDouble()).toFloat().coerceIn(0f, 1f) }

    val BT2020_TO_BT709 = floatArrayOf(1.6605f, -0.5876f, -0.0728f, -0.1246f, 1.1329f, -0.0083f, -0.0182f, -0.1006f, 1.1187f)
    val P3_TO_BT709 = floatArrayOf(1.2249f, -0.2249f, 0f, -0.0420f, 1.0420f, 0f, -0.0196f, -0.0786f, 1.0983f)

    val DCI_P3_TO_BT709 = floatArrayOf(1.157516f, -0.154962f, -0.002554f, -0.041500f, 1.045568f, -0.004068f, -0.018050f, -0.078578f, 1.096628f)

    fun matrixFor(gamutClass: Int): FloatArray = when (gamutClass) {
        2 -> P3_TO_BT709
        3 -> DCI_P3_TO_BT709
        else -> BT2020_TO_BT709
    }

    fun bt2020ToBt709(r: Float, g: Float, b: Float, out: FloatArray) = convert(BT2020_TO_BT709, r, g, b, out)

    fun convert(m: FloatArray, r: Float, g: Float, b: Float, out: FloatArray) {
        val lr = decodeTable[(r.coerceIn(0f, 1f) * DECODE_MAX + 0.5f).toInt()]
        val lg = decodeTable[(g.coerceIn(0f, 1f) * DECODE_MAX + 0.5f).toInt()]
        val lb = decodeTable[(b.coerceIn(0f, 1f) * DECODE_MAX + 0.5f).toInt()]
        var cr = m[0] * lr + m[1] * lg + m[2] * lb
        var cg = m[3] * lr + m[4] * lg + m[5] * lb
        var cb = m[6] * lr + m[7] * lg + m[8] * lb
        val y = (0.2126f * cr + 0.7152f * cg + 0.0722f * cb).coerceIn(0f, 1f)
        val low = min(cr, min(cg, cb))
        if (low < 0f) {
            val t = if (y - low > MIN_LUMA) y / (y - low) else 0f
            cr = y + (cr - y) * t; cg = y + (cg - y) * t; cb = y + (cb - y) * t
        }
        val high = max(cr, max(cg, cb))
        if (high > 1f) {
            val t = if (high - y > MIN_LUMA) (1f - y) / (high - y) else 0f
            cr = y + (cr - y) * t; cg = y + (cg - y) * t; cb = y + (cb - y) * t
        }
        out[0] = encodeTable[(cr.coerceIn(0f, 1f) * ENCODE_MAX + 0.5f).toInt()]
        out[1] = encodeTable[(cg.coerceIn(0f, 1f) * ENCODE_MAX + 0.5f).toInt()]
        out[2] = encodeTable[(cb.coerceIn(0f, 1f) * ENCODE_MAX + 0.5f).toInt()]
    }

    private fun srgbDecode(v: Double): Double = if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

    private fun srgbEncode(v: Double): Double = if (v <= 0.0031308) v * 12.92 else 1.055 * v.pow(1.0 / 2.4) - 0.055
}
