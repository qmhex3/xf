package com.frameout.core.edit

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object CoverPainter {
    private const val BLUR_CELLS_SOFT = 28f
    private const val BLUR_CELLS_HARD = 2f
    private const val PIXEL_CELLS_SOFT = 48f
    private const val PIXEL_CELLS_HARD = 4f

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
    private fun cells(style: CoverStyle, strength: Float): Float =
        if (style == CoverStyle.Blur) lerp(BLUR_CELLS_SOFT, BLUR_CELLS_HARD, strength)
        else lerp(PIXEL_CELLS_SOFT, PIXEL_CELLS_HARD, strength)

    fun paint(canvas: Canvas, bitmap: Bitmap, mark: CoverMark) {
        val rect = pixelRect(bitmap, mark) ?: return
        when (mark.style) {
            CoverStyle.Solid -> canvas.drawRect(rect, Paint().apply { color = Color.BLACK })
            else -> scaled(canvas, bitmap, rect, cells(mark.style, mark.strength), mark.style == CoverStyle.Blur)
        }
    }

    fun paint(canvas: Canvas, bitmap: Bitmap, mark: BrushCoverMark, referenceMin: Float = 0f) {
        if (mark.points.isEmpty()) return
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = (mark.width * min(w, h)).coerceAtLeast(2f)
        }
        var effect: Bitmap? = null
        if (mark.style == CoverStyle.Solid) {
            paint.color = Color.BLACK
        } else {
            effect = fullSizeEffect(bitmap, cells(mark.style, mark.strength), mark.style == CoverStyle.Blur, referenceMin)
            paint.shader = BitmapShader(effect, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val path = Path()
        val first = mark.points.first()
        path.moveTo(first.x * w, first.y * h)
        if (mark.points.size == 1) path.lineTo(first.x * w + 0.01f, first.y * h)
        mark.points.drop(1).forEach { path.lineTo(it.x * w, it.y * h) }
        canvas.drawPath(path, paint)
        effect?.recycle()
    }

    private fun fullSizeEffect(bitmap: Bitmap, cells: Float, smooth: Boolean, referenceMin: Float): Bitmap {
        val reference = if (referenceMin > 0f) referenceMin else min(bitmap.width, bitmap.height).toFloat()
        val factor = min(1f, cells / reference)
        val tw = (bitmap.width * factor).roundToInt().coerceAtLeast(1)
        val th = (bitmap.height * factor).roundToInt().coerceAtLeast(1)
        val small = shrink(bitmap, tw, th)
        val full = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, smooth)
        if (small !== bitmap && small !== full) small.recycle()
        return full
    }

    private fun pixelRect(bitmap: Bitmap, mark: CoverMark): Rect? {
        val l = (min(mark.left, mark.right) * bitmap.width).roundToInt()
        val t = (min(mark.top, mark.bottom) * bitmap.height).roundToInt()
        val r = (max(mark.left, mark.right) * bitmap.width).roundToInt()
        val b = (max(mark.top, mark.bottom) * bitmap.height).roundToInt()
        if (r <= 0 || l >= bitmap.width || b <= 0 || t >= bitmap.height) return null
        val left = l.coerceIn(0, bitmap.width - 1)
        val top = t.coerceIn(0, bitmap.height - 1)
        val right = r.coerceIn(left + 1, bitmap.width)
        val bottom = b.coerceIn(top + 1, bitmap.height)
        return Rect(left, top, right, bottom).takeIf { !it.isEmpty }
    }

    private fun scaled(canvas: Canvas, bitmap: Bitmap, rect: Rect, cells: Float, smooth: Boolean) {
        val region = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
        val factor = min(1f, cells / min(rect.width(), rect.height()))
        val tw = (rect.width() * factor).roundToInt().coerceAtLeast(1)
        val th = (rect.height() * factor).roundToInt().coerceAtLeast(1)
        val small = shrink(region, tw, th)
        val paint = Paint().apply { isFilterBitmap = smooth }
        canvas.drawBitmap(small, null, rect, paint)
        if (small !== region) small.recycle()
        region.recycle()
    }

    private fun shrink(source: Bitmap, tw: Int, th: Int): Bitmap {
        var current = source
        while (current.width / 2 >= tw && current.height / 2 >= th) {
            val next = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
            if (current !== source) current.recycle()
            current = next
        }
        val result = Bitmap.createScaledBitmap(current, tw, th, true)
        if (current !== source && current !== result) current.recycle()
        return result
    }
}
