package com.frameout.app.compare

import com.frameout.core.util.SmoothScale
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal object DiffMaker {
    class Result(val bitmap: Bitmap, val percent: Float)

    private const val MAX_SIDE = 2048
    private const val CHANGED_DELTA = 0.02f
    private const val SATURATED_DELTA = 0.25f

    private val linear = FloatArray(256) { v ->
        val c = v / 255f
        if (c <= 0.04045f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    }

    fun make(a: Bitmap, b: Bitmap): Result? {
        if (a.isRecycled || b.isRecycled) return null
        val scale = min(1f, MAX_SIDE.toFloat() / max(a.width, a.height))
        val w = max(1, (a.width * scale).toInt())
        val h = max(1, (a.height * scale).toInt())
        val pa = pixels(a, w, h) ?: return null
        val pb = pixels(b, w, h) ?: return null
        val out = IntArray(w * h)
        val lab = FloatArray(3)
        val labB = FloatArray(3)
        var changed = 0L
        var compared = 0L
        for (i in out.indices) {
            val ca = pa[i]
            val cb = pb[i]
            if ((ca ushr 24) == 0 || (cb ushr 24) == 0) {
                out[i] = EMPTY
                continue
            }
            compared++
            if (ca == cb) {
                out[i] = ZERO
                continue
            }
            oklab(ca, lab)
            oklab(cb, labB)
            val dl = lab[0] - labB[0]
            val da = lab[1] - labB[1]
            val db = lab[2] - labB[2]
            val d = sqrt(dl * dl + da * da + db * db)
            if (d > CHANGED_DELTA) changed++
            val v = min(255, (d / SATURATED_DELTA * 255f).toInt())
            out[i] = (0xFF shl 24) or (min(255, v * 2) shl 16) or ((v * v / 255) shl 8) or (v / 3)
        }
        val bitmap = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
        return Result(bitmap, if (compared == 0L) 0f else changed * 100f / compared)
    }

    private const val ZERO = 0xFF000000.toInt()
    private const val EMPTY = 0xFF1A1A1A.toInt()

    private fun oklab(argb: Int, out: FloatArray) {
        val r = linear[(argb shr 16) and 0xFF]
        val g = linear[(argb shr 8) and 0xFF]
        val b = linear[argb and 0xFF]
        val l = Math.cbrt((0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b).toDouble()).toFloat()
        val m = Math.cbrt((0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b).toDouble()).toFloat()
        val s = Math.cbrt((0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b).toDouble()).toFloat()
        out[0] = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s
        out[1] = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s
        out[2] = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
    }

    private fun isHardware(src: Bitmap): Boolean = Build.VERSION.SDK_INT >= 26 && src.config == Bitmap.Config.HARDWARE

    private fun pixels(src: Bitmap, w: Int, h: Int): IntArray? = runCatching {
        val readable = if (isHardware(src)) src.copy(Bitmap.Config.ARGB_8888, false) else src
        val fit = min(w / readable.width.toFloat(), h / readable.height.toFloat())
        var fw = max(1, Math.round(readable.width * fit).coerceAtMost(w))
        var fh = max(1, Math.round(readable.height * fit).coerceAtMost(h))
        if (w - fw <= 1 && h - fh <= 1) { fw = w; fh = h }
        val scaled = if (readable.width == fw && readable.height == fh) readable else SmoothScale.scaled(readable, fw, fh, true)
        val data = IntArray(w * h)
        if (fw == w && fh == h) {
            scaled.getPixels(data, 0, w, 0, 0, w, h)
        } else {
            val canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(canvasBitmap).drawBitmap(scaled, ((w - fw) / 2).toFloat(), ((h - fh) / 2).toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
            canvasBitmap.getPixels(data, 0, w, 0, 0, w, h)
            canvasBitmap.recycle()
        }
        if (scaled !== readable) scaled.recycle()
        if (readable !== src) readable.recycle()
        data
    }.getOrNull()
}
