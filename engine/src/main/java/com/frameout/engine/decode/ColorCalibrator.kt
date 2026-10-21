package com.frameout.engine.decode

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.frameout.core.util.DiagLog
import java.util.concurrent.ConcurrentHashMap

internal object ColorCalibrator {
    @Volatile var enabled = true

    private const val GRID = 24
    private const val SAMPLES = 4
    private const val MAX_ATTEMPTS = 3

    private val chosen = ConcurrentHashMap<String, Int>()
    private val attempts = ConcurrentHashMap<String, Int>()

    fun cached(uri: Uri): Int? = chosen[uri.toString()]

    fun wanted(uri: Uri): Boolean {
        val key = uri.toString()
        return enabled && !chosen.containsKey(key) && (attempts[key] ?: 0) < MAX_ATTEMPTS
    }

    fun forget(uri: Uri) {
        chosen.remove(uri.toString())
        attempts.remove(uri.toString())
    }

    fun calibrate(context: Context, uri: Uri, ptsUs: Long, render: (Int) -> Bitmap?): Int? {
        val key = uri.toString()
        attempts.merge(key, 1, Int::plus)
        val reference = referenceSignature(context, uri, ptsUs) ?: return null
        val autoError = errorOf(render(ColorVariants.AUTO), reference)
        if (autoError.isNaN()) return null
        if (autoError <= CalibrationRule.ACCEPT) return remember(key, ColorVariants.AUTO, autoError, emptyMap())
        val errors = LinkedHashMap<Int, Float>()
        for (id in ColorVariants.CANDIDATES) {
            val error = errorOf(render(id), reference)
            if (!error.isNaN()) errors[id] = error
            if (error <= CalibrationRule.ACCEPT) break
        }
        val pick = CalibrationRule.pick(autoError, errors) ?: run {
            DiagLog.append("ColorCalibrator", "inconclusive auto=$autoError errors=$errors")
            return null
        }
        return remember(key, pick, autoError, errors)
    }

    private fun remember(key: String, id: Int, autoError: Float, errors: Map<Int, Float>): Int {
        chosen[key] = id
        DiagLog.append("ColorCalibrator", "variant=$id (${ColorVariants.describe(id)}) autoError=$autoError candidates=$errors")
        return id
    }

    private fun errorOf(bitmap: Bitmap?, reference: FloatArray): Float {
        if (bitmap == null) return Float.NaN
        return try {
            CalibrationRule.meanAbsDifference(signature(bitmap), reference)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun referenceSignature(context: Context, uri: Uri, ptsUs: Long): FloatArray? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(ptsUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: return@runCatching null
            val plain = FallbackColor.normalize(frame)
            try { signature(plain) } finally { if (!plain.isRecycled) plain.recycle() }
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull()

    internal fun signature(bitmap: Bitmap): FloatArray {
        val out = FloatArray(GRID * GRID * 3)
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0 || bitmap.isRecycled) return out
        val total = GRID * SAMPLES
        for (gy in 0 until GRID) for (gx in 0 until GRID) {
            var r = 0f
            var g = 0f
            var b = 0f
            for (sy in 0 until SAMPLES) for (sx in 0 until SAMPLES) {
                val x = (((gx * SAMPLES + sx) + 0.5f) * w / total).toInt().coerceIn(0, w - 1)
                val y = (((gy * SAMPLES + sy) + 0.5f) * h / total).toInt().coerceIn(0, h - 1)
                val p = bitmap.getPixel(x, y)
                r += (p shr 16) and 0xFF
                g += (p shr 8) and 0xFF
                b += p and 0xFF
            }
            val at = (gy * GRID + gx) * 3
            val n = (SAMPLES * SAMPLES).toFloat()
            out[at] = r / n
            out[at + 1] = g / n
            out[at + 2] = b / n
        }
        return out
    }
}
