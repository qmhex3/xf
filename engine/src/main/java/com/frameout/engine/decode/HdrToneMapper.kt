package com.frameout.engine.decode

import android.media.MediaFormat
import kotlin.math.ln
import kotlin.math.pow

internal class HdrToneMapper(private val params: ColorParams, desaturation: Float = HdrTables.DESAT_STRENGTH, private val trackGain: Boolean = false) {
    private val hlg = params.transfer == MediaFormat.COLOR_TRANSFER_HLG
    private val ootf = HdrTables.ootf
    private val peak = if (hlg) HdrTables.HLG_PEAK else params.peakNits
    private val white = (peak / HdrTables.REF_WHITE).coerceAtLeast(2f)
    private val headroom = 1f - HdrTables.TONE_KNEE
    private val span = (white - HdrTables.TONE_KNEE) / headroom
    private val out = FloatArray(3)
    private val desatTable = FloatArray(HdrTables.DESAT_STEPS + 1) { desaturation * (it / HdrTables.DESAT_STEPS.toFloat()).pow(HdrTables.DESAT_EXPONENT) }

    val gainCodes = IntArray(3)

    private val raw = FloatArray(3)

    fun map(r: Float, g: Float, b: Float): Int = mapAt(r, g, b, 0.5f)

    fun map(r: Float, g: Float, b: Float, x: Int, y: Int): Int = mapAt(r, g, b, Dither.threshold(x, y))

    private fun mapAt(r: Float, g: Float, b: Float, t: Float): Int {
        var lr = decode(r)
        var lg = decode(g)
        var lb = decode(b)
        if (hlg) {
            val k = ootf[HdrTables.index(luma(lr, lg, lb))]
            lr *= k; lg *= k; lb *= k
        }
        val source = luma(lr, lg, lb)
        val target = toneLuma(source)
        val scale = if (source > MIN_LUMA) target / source else 1f
        if (trackGain) toDisplayGamut(lr, lg, lb, raw)
        val weight = desaturation(maxOf(lr, lg, lb))
        val keep = scale * (1f - weight)
        val gray = target * weight
        toDisplayGamut(lr * keep + gray, lg * keep + gray, lb * keep + gray, out)
        fitGamut(target.coerceIn(0f, 1f))
        val cr = encode(out[0], t)
        val cg = encode(out[1], t)
        val cb = encode(out[2], t)
        if (trackGain) {
            gainCodes[0] = gainCode(raw[0], cr)
            gainCodes[1] = gainCode(raw[1], cg)
            gainCodes[2] = gainCode(raw[2], cb)
        }
        return -0x1000000 or (cr shl 16) or (cg shl 8) or cb
    }

    fun toneLuma(y: Float): Float {
        if (y <= HdrTables.TONE_KNEE) return y.coerceAtLeast(0f)
        val u = (y - HdrTables.TONE_KNEE) / headroom
        val mapped = HdrTables.TONE_KNEE + headroom * u * (1f + u / (span * span)) / (1f + u)
        return mapped.coerceAtMost(1f)
    }

    fun desaturation(y: Float): Float {
        if (y <= HdrTables.DESAT_KNEE) return 0f
        val t = ((y - HdrTables.DESAT_KNEE) / (white - HdrTables.DESAT_KNEE)).coerceIn(0f, 1f)
        return desatTable[(t * HdrTables.DESAT_STEPS + 0.5f).toInt()]
    }

    private fun decode(signal: Float): Float {
        val i = HdrTables.index(signal)
        return if (hlg) HdrTables.hlg[i] else HdrTables.pq[i]
    }

    private val gamutClass = params.gamutClass
    private val lumaWeights = when (gamutClass) {
        0 -> floatArrayOf(0.2126f, 0.7152f, 0.0722f)
        2 -> floatArrayOf(0.2290f, 0.6917f, 0.0793f)
        3 -> floatArrayOf(0.2152f, 0.7091f, 0.0757f)
        else -> floatArrayOf(0.2627f, 0.6780f, 0.0593f)
    }
    private val display = GamutConvert.matrixFor(gamutClass)

    private fun luma(r: Float, g: Float, b: Float): Float = lumaWeights[0] * r + lumaWeights[1] * g + lumaWeights[2] * b

    private fun toDisplayGamut(r: Float, g: Float, b: Float, dst: FloatArray) {
        if (gamutClass == 0) {
            dst[0] = r; dst[1] = g; dst[2] = b
        } else {
            dst[0] = display[0] * r + display[1] * g + display[2] * b
            dst[1] = display[3] * r + display[4] * g + display[5] * b
            dst[2] = display[6] * r + display[7] * g + display[8] * b
        }
    }

    private fun gainCode(hdr: Float, code: Int): Int {
        if (hdr < 0f) return GainCodec.codeOf(0f)
        return GainCodec.codeOf(ln((hdr + EPS) / (srgbLinear[code] + EPS)))
    }

    private fun fitGamut(y: Float) {
        val low = minOf(out[0], out[1], out[2])
        if (low < 0f) blend(y, if (y - low > MIN_LUMA) y / (y - low) else 0f)
        val high = maxOf(out[0], out[1], out[2])
        if (high > 1f) fitHigh(y, high)
    }

    private fun fitHigh(y: Float, high: Float) {
        val t = (1f - y) / (high - y)
        for (i in 0 until 3) {
            val toGrey = y + (out[i] - y) * t
            out[i] = (1f - CHROMA_KEEP) * toGrey + CHROMA_KEEP * (out[i] / high)
        }
    }

    private fun blend(y: Float, t: Float) {
        for (i in 0 until 3) out[i] = y + (out[i] - y) * t
    }

    private fun encode(linear: Float, t: Float): Int = (HdrTables.srgb[HdrTables.index(linear)] + t).toInt().coerceAtMost(255)

    private companion object {
        const val CHROMA_KEEP = 0.75f
        const val EPS = 1f / 64f
        const val MIN_LUMA = 1e-6f
        val srgbLinear = FloatArray(256) {
            val v = it / 255f
            if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        }
    }
}
