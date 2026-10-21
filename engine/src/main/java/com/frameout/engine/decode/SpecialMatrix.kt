package com.frameout.engine.decode

import android.media.MediaFormat
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

internal object SpecialMatrix {
    private const val PQ_M1 = 0.1593017578125f
    private const val PQ_M2 = 78.84375f
    private const val PQ_C1 = 0.8359375f
    private const val PQ_C2 = 18.8515625f
    private const val PQ_C3 = 18.6875f
    private const val HLG_A = 0.17883277f
    private const val HLG_B = 0.28466892f
    private const val HLG_C = 0.55991073f
    private const val BT_ALPHA = 1.09929682680944f
    private const val BT_BETA = 0.018053968510807f

    fun convert(params: ColorParams, y: Float, cb: Float, cr: Float, out: FloatArray) {
        when (params.special) {
            ColorParams.MATRIX_YCGCO -> {
                out[0] = y - cb + cr
                out[1] = y + cb
                out[2] = y - cb - cr
            }
            ColorParams.MATRIX_BT2020_CL -> constantLuminance(y, cb, cr, out)
            else -> ictcp(y, cb, cr, params.transfer == MediaFormat.COLOR_TRANSFER_HLG, out)
        }
        out[0] = out[0].coerceIn(0f, 1f)
        out[1] = out[1].coerceIn(0f, 1f)
        out[2] = out[2].coerceIn(0f, 1f)
    }

    private fun constantLuminance(yc: Float, cb: Float, cr: Float, out: FloatArray) {
        val bp = yc + cb * (if (cb <= 0f) 1.9404f else 1.5816f)
        val rp = yc + cr * (if (cr <= 0f) 1.7184f else 0.9936f)
        val luminance = bt2020Eotf(yc)
        val r = bt2020Eotf(rp)
        val b = bt2020Eotf(bp)
        val g = max((luminance - 0.2627f * r - 0.0593f * b) / 0.6780f, 0f)
        out[0] = rp
        out[1] = bt2020Oetf(g)
        out[2] = bp
    }

    private fun ictcp(i: Float, ct: Float, cp: Float, hlg: Boolean, out: FloatArray) {
        val lp = i + 0.00860903703793281f * ct + 0.111029625003026f * cp
        val mp = i - 0.00860903703793276f * ct - 0.111029625003026f * cp
        val sp = i + 0.560031335710679f * ct - 0.320627174987319f * cp
        val l: Float
        val m: Float
        val s: Float
        if (hlg) { l = hlgEotf(lp); m = hlgEotf(mp); s = hlgEotf(sp) } else { l = pqEotf(lp); m = pqEotf(mp); s = pqEotf(sp) }
        val r = 3.436606694333078f * l - 2.506452118656270f * m + 0.069845424323191f * s
        val g = -0.791329555598929f * l + 1.983600451792291f * m - 0.192270896193362f * s
        val b = -0.0259499f * l - 0.09891371f * m + 1.12486361f * s
        if (hlg) {
            out[0] = hlgOetf(r); out[1] = hlgOetf(g); out[2] = hlgOetf(b)
        } else {
            out[0] = pqOetf(r); out[1] = pqOetf(g); out[2] = pqOetf(b)
        }
    }

    private fun pqEotf(v: Float): Float {
        val p = v.coerceIn(0f, 1f).pow(1f / PQ_M2)
        val den = PQ_C2 - PQ_C3 * p
        if (den <= 0f) return 1f
        return (max(p - PQ_C1, 0f) / den).pow(1f / PQ_M1)
    }

    private fun pqOetf(l: Float): Float {
        val p = l.coerceIn(0f, 1f).pow(PQ_M1)
        return ((PQ_C1 + PQ_C2 * p) / (1f + PQ_C3 * p)).pow(PQ_M2)
    }

    private fun hlgEotf(v: Float): Float {
        val e = v.coerceIn(0f, 1f)
        return if (e <= 0.5f) e * e / 3f else (exp((e - HLG_C) / HLG_A) + HLG_B) / 12f
    }

    private fun hlgOetf(v: Float): Float {
        val l = v.coerceIn(0f, 1f)
        return if (l <= 1f / 12f) sqrt(3f * l) else HLG_A * ln(12f * l - HLG_B) + HLG_C
    }

    private fun bt2020Oetf(v: Float): Float {
        val l = max(v, 0f)
        return if (l < BT_BETA) 4.5f * l else BT_ALPHA * l.pow(0.45f) - (BT_ALPHA - 1f)
    }

    private fun bt2020Eotf(v: Float): Float {
        val e = max(v, 0f)
        return if (e < 4.5f * BT_BETA) e / 4.5f else ((e + BT_ALPHA - 1f) / BT_ALPHA).pow(1f / 0.45f)
    }
}
