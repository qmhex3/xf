package com.frameout.core.edit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object ShapeArrows {
    private fun headLength(g: ShapeGeo, len: Float, scale: Float = 1f): Float =
        min(max(g.sw * 4.2f, g.ref * 0.028f) * scale, len * 0.6f)

    private fun headHalf(g: ShapeGeo): Float = max(g.sw * 2.1f, g.ref * 0.014f)

    private fun head(canvas: Canvas, color: Int, g: ShapeGeo, tx: Float, ty: Float, ux: Float, uy: Float, hl: Float, hw: Float) {
        val bx = tx - ux * hl
        val by = ty - uy * hl
        val path = Path().apply {
            moveTo(tx, ty)
            lineTo(bx - uy * hw, by + ux * hw)
            lineTo(bx + uy * hw, by - ux * hw)
            close()
        }
        val paint = ShapePaints.solid(color).apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = g.sw * 0.4f
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, paint)
    }

    fun straight(canvas: Canvas, m: ShapeMark, g: ShapeGeo) {
        val dx = g.x2 - g.x1
        val dy = g.y2 - g.y1
        val len = hypot(dx, dy)
        if (len < 1f) return
        val ux = dx / len
        val uy = dy / len
        val hl = headLength(g, len)
        val both = m.kind == ShapeKind.DoubleArrow
        val arrowed = m.kind != ShapeKind.Line
        val startInset = if (both) hl * 0.85f else 0f
        val endInset = if (arrowed) hl * 0.85f else 0f
        canvas.drawLine(g.x1 + ux * startInset, g.y1 + uy * startInset, g.x2 - ux * endInset, g.y2 - uy * endInset, ShapePaints.stroke(m, g.sw))
        if (arrowed) head(canvas, m.color, g, g.x2, g.y2, ux, uy, hl, headHalf(g))
        if (both) head(canvas, m.color, g, g.x1, g.y1, -ux, -uy, hl, headHalf(g))
        if (m.kind == ShapeKind.DotArrow) canvas.drawCircle(g.x1, g.y1, g.sw * 1.8f, ShapePaints.solid(m.color))
    }

    fun taper(canvas: Canvas, m: ShapeMark, g: ShapeGeo) {
        val dx = g.x2 - g.x1
        val dy = g.y2 - g.y1
        val len = hypot(dx, dy)
        if (len < 1f) return
        val ux = dx / len
        val uy = dy / len
        val hl = headLength(g, len, 1.2f)
        val hw = headHalf(g) * 1.15f
        val bx = g.x2 - ux * hl
        val by = g.y2 - uy * hl
        val shaft = g.sw * 0.55f
        val path = Path().apply {
            moveTo(g.x1, g.y1)
            lineTo(bx - uy * shaft, by + ux * shaft)
            lineTo(bx - uy * hw, by + ux * hw)
            lineTo(g.x2, g.y2)
            lineTo(bx + uy * hw, by - ux * hw)
            lineTo(bx + uy * shaft, by - ux * shaft)
            close()
        }
        val paint = ShapePaints.solid(m.color).apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = g.sw * 0.25f
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, paint)
    }

    fun curved(canvas: Canvas, m: ShapeMark, g: ShapeGeo) {
        val dx = g.x2 - g.x1
        val dy = g.y2 - g.y1
        val len = hypot(dx, dy)
        if (len < 1f) return
        val ctrlX = (g.x1 + g.x2) / 2f - dy * 0.28f
        val ctrlY = (g.y1 + g.y2) / 2f + dx * 0.28f
        val tl = hypot(g.x2 - ctrlX, g.y2 - ctrlY).coerceAtLeast(0.001f)
        val tux = (g.x2 - ctrlX) / tl
        val tuy = (g.y2 - ctrlY) / tl
        val hl = headLength(g, len)
        val path = Path().apply {
            moveTo(g.x1, g.y1)
            quadTo(ctrlX, ctrlY, g.x2 - tux * hl * 0.85f, g.y2 - tuy * hl * 0.85f)
        }
        canvas.drawPath(path, ShapePaints.stroke(m, g.sw))
        head(canvas, m.color, g, g.x2, g.y2, tux, tuy, hl, headHalf(g))
    }

    fun measure(canvas: Canvas, m: ShapeMark, g: ShapeGeo, pixelW: Float, compact: Boolean) {
        val dx = g.x2 - g.x1
        val dy = g.y2 - g.y1
        val len = hypot(dx, dy)
        if (len < 1f) return
        val ux = dx / len
        val uy = dy / len
        val tick = max(g.sw * 2.6f, g.ref * 0.02f)
        canvas.drawLine(g.x1, g.y1, g.x2, g.y2, ShapePaints.stroke(m, g.sw))
        val ends = ShapePaints.stroke(m, g.sw, dash = false)
        canvas.drawLine(g.x1 - uy * tick, g.y1 + ux * tick, g.x1 + uy * tick, g.y1 - ux * tick, ends)
        canvas.drawLine(g.x2 - uy * tick, g.y2 + ux * tick, g.x2 + uy * tick, g.y2 - ux * tick, ends)
        if (compact) return
        val px = hypot(dx * pixelW / g.w, dy * pixelW / g.w).roundToInt()
        label(canvas, "$px px", (g.x1 + g.x2) / 2f, (g.y1 + g.y2) / 2f, max(g.ref * 0.032f, g.sw * 2.6f), m.color)
    }

    private fun label(canvas: Canvas, text: String, cx: Float, cy: Float, size: Float, accent: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val half = paint.measureText(text) / 2f + size * 0.55f
        val rect = RectF(cx - half, cy - size * 0.95f, cx + half, cy + size * 0.95f)
        canvas.drawRoundRect(rect, size, size, ShapePaints.solid(0xFF000000.toInt(), 0.72f))
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, size * 0.07f)
            color = accent
        }
        canvas.drawRoundRect(rect, size, size, rim)
        canvas.drawText(text, cx, cy - (paint.ascent() + paint.descent()) / 2f, paint)
    }
}
