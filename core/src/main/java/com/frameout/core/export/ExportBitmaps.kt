package com.frameout.core.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

object ExportBitmaps {
    fun load(path: String?, maxSide: Int): Bitmap? {
        if (path == null) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun fitSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
        val scale = minOf(1f, maxSide.toFloat() / maxOf(width, height))
        return (width * scale).toInt().coerceAtLeast(2) to (height * scale).toInt().coerceAtLeast(2)
    }

    fun letterbox(src: Bitmap, width: Int, height: Int): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(0xFF000000.toInt())
        val scale = minOf(width.toFloat() / src.width, height.toFloat() / src.height)
        val w = src.width * scale
        val h = src.height * scale
        val dest = RectF((width - w) / 2f, (height - h) / 2f, (width + w) / 2f, (height + h) / 2f)
        canvas.drawBitmap(src, null, dest, Paint(Paint.FILTER_BITMAP_FLAG))
        return result
    }
}
