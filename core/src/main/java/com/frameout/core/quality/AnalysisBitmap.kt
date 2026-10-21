package com.frameout.core.quality

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.frameout.core.util.SmoothScale
import com.frameout.core.util.ThumbCache
import java.io.File

internal object AnalysisBitmap {
    fun load(paths: List<String>, side: Int): Bitmap? {
        for (path in paths) {
            val bitmap = decode(path, side)
            if (bitmap != null) return bitmap
        }
        return null
    }

    private fun decode(path: String, side: Int): Bitmap? {
        val file = File(path)
        if (!file.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = AnalysisScale.sampleFor(bounds.outWidth, bounds.outHeight, side)
        val width = (bounds.outWidth + sample - 1) / sample
        val height = (bounds.outHeight + sample - 1) / sample
        val decoded = ThumbCache.pool.decodeFile(file.absolutePath, sample, width, height) ?: return null
        return fit(decoded, side)
    }

    private fun fit(decoded: Bitmap, side: Int): Bitmap {
        val (w, h) = AnalysisScale.target(decoded.width, decoded.height, side)
        if (w == decoded.width && h == decoded.height) return decoded
        val scaled = SmoothScale.scaled(decoded, w, h, true)
        if (scaled !== decoded) ThumbCache.release(decoded)
        return scaled
    }
}
