package com.frameout.engine.work

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import java.util.concurrent.ConcurrentHashMap

internal object ExtractStrip {
    private const val CELLS = 24
    private const val WIDTH_PX = 480
    private const val GAP_PX = 3f
    private const val EMPTY = 0x408A8A95
    private const val THUMB_PX = 112

    private val thumbs = ConcurrentHashMap<Long, Bitmap>()

    fun render(context: Context, fraction: Float, heightPx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH_PX, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val accent = NotificationTheme.accent(context)
        val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val filled = (fraction.coerceIn(0f, 1f) * CELLS).toInt()
        val cell = (WIDTH_PX - GAP_PX * (CELLS - 1)) / CELLS
        for (i in 0 until CELLS) {
            paint.color = when {
                i < filled -> accent.strong
                i == filled && fraction < 1f -> accent.soft
                else -> EMPTY
            }
            val slot = if (rtl) CELLS - 1 - i else i
            val left = slot * (cell + GAP_PX)
            canvas.drawRoundRect(RectF(left, 0f, left + cell, heightPx.toFloat()), 3f, 3f, paint)
        }
        return bitmap
    }

    fun thumb(projectId: Long, path: String?): Bitmap? {
        thumbs[projectId]?.let { return it }
        if (path.isNullOrBlank()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= THUMB_PX) sample *= 2
        val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        thumbs[projectId] = decoded
        return decoded
    }

    fun forget(projectId: Long) {
        thumbs.remove(projectId)
    }
}
