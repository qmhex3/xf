package com.frameout.core.edit

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object ShapePainter {
    private const val DIM = 0.66f
    private const val ZOOM = 2.2f
    private const val HIGHLIGHT = 0.55f

    fun paint(canvas: Canvas, m: ShapeMark, w: Float, h: Float, source: Bitmap? = null, pixelW: Float = w, compact: Boolean = false) {
        if (w <= 0f || h <= 0f) return
        val g = ShapeGeo(m, w, h)
        val layered = m.opacity < 0.995f
        val saved = if (layered) canvas.saveLayerAlpha(0f, 0f, w, h, (m.opacity.coerceIn(0f, 1f) * 255f).roundToInt()) else -1
        when (m.kind) {
            ShapeKind.Arrow, ShapeKind.DotArrow, ShapeKind.DoubleArrow, ShapeKind.Line -> ShapeArrows.straight(canvas, m, g)
            ShapeKind.TaperArrow -> ShapeArrows.taper(canvas, m, g)
            ShapeKind.CurvedArrow -> ShapeArrows.curved(canvas, m, g)
            ShapeKind.Measure -> ShapeArrows.measure(canvas, m, g, pixelW, compact)
            ShapeKind.Rect, ShapeKind.RoundRect, ShapeKind.Ellipse, ShapeKind.Triangle, ShapeKind.Diamond, ShapeKind.Star -> closed(canvas, m, g)
            ShapeKind.Bracket -> bracket(canvas, m, g)
            ShapeKind.Highlighter -> highlighter(canvas, m, g)
            ShapeKind.Spotlight, ShapeKind.SpotlightRect -> spotlight(canvas, m, g)
            ShapeKind.Magnifier -> magnifier(canvas, m, g, source)
            ShapeKind.Number -> number(canvas, m, g)
            ShapeKind.Callout -> callout(canvas, m, g)
            ShapeKind.Check, ShapeKind.Cross -> badge(canvas, m, g)
        }
        if (layered) canvas.restoreToCount(saved)
    }

    private fun starPath(g: ShapeGeo): Path = Path().apply {
        for (i in 0 until 10) {
            val k = if (i % 2 == 0) 1f else 0.42f
            val a = -PI / 2.0 + i * PI / 5.0
            val px = g.cx + (cos(a) * g.bw / 2f * k).toFloat()
            val py = g.cy + (sin(a) * g.bh / 2f * k).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

    private fun polygon(vararg p: Float): Path = Path().apply {
        moveTo(p[0], p[1])
        for (i in 2 until p.size step 2) lineTo(p[i], p[i + 1])
        close()
    }

    private fun shapePath(kind: ShapeKind, g: ShapeGeo): Path {
        val rect = RectF(g.l, g.t, g.r, g.b)
        val corner = min(g.bw, g.bh) * 0.22f
        return when (kind) {
            ShapeKind.Rect -> Path().apply { addRect(rect, Path.Direction.CW) }
            ShapeKind.RoundRect, ShapeKind.SpotlightRect, ShapeKind.Callout -> Path().apply { addRoundRect(rect, corner, corner, Path.Direction.CW) }
            ShapeKind.Ellipse -> Path().apply { addOval(rect, Path.Direction.CW) }
            ShapeKind.Triangle -> polygon(g.cx, g.t, g.r, g.b, g.l, g.b)
            ShapeKind.Diamond -> polygon(g.cx, g.t, g.r, g.cy, g.cx, g.b, g.l, g.cy)
            else -> starPath(g)
        }
    }

    private fun closed(canvas: Canvas, m: ShapeMark, g: ShapeGeo) {
        if (g.bw < 1f || g.bh < 1f) return
        val path = shapePath(m.kind, g)
        val f = ShapePaints.fillFactor(m)
        if (f > 0f) canvas.drawPath(path, ShapePaints.solid(m.color, f))
        canvas.drawPath(path, ShapePaints.stroke(m, g.sw))
    }

    private fun bracket(canvas: Canvas, m: ShapeMark, g: ShapeGeo) {
        if (g.bw < 1f && g.bh < 1f) return
        val vertical = g.bh >= g.bw
        val from = if (vertical) g.t else g.l
        val to = if (vertical) g.b else g.r
        val outer = if (vertical) g.r else g.b
        val tip = if (vertical) g.l else g.t
        val mid = (from + to) / 2f
        val depth = (outer + tip) / 2f
        val q = min(abs(to - from) * 0.14f, abs(outer - tip) * 0.5f).coerceAtLeast(1f)
        val path = Path()
        fun x(a: Float, d: Float) = if (vertical) d else a
        fun y(a: Float, d: Float) = if (vertical) a else d
        path.moveTo(x(from, outer), y(from, outer))
        path.quadTo(x(from, depth), y(from, depth), x(from + q, depth), y(from + q, depth))
        path.lineTo(x(mid - q, depth), y(mid - q, depth))
        path.quadTo(x(mid, depth), y(mid, depth), x(mid, tip), y(mid, tip))
        path.quadTo(x(mid, depth), y(mid, depth), x(mid + q, depth), y(mid + q, depth))
        path.lineTo(x(to - q, depth), y(to - q, depth))
        path.quadTo(x(to, depth), y(to, depth), x(to, outer), y(to, outer))
        canvas.drawPath(path, ShapePaints.stroke(m, g.sw))
    }

    private fun highlighter(canvas: Canvas, m: ShapeMark, g: ShapeGeo) {
        if (g.bw < 1f || g.bh < 1f) return
        val r = min(g.bw, g.bh) * 0.25f
        canvas.drawRoundRect(RectF(g.l, g.t, g.r, g.b), r, r, ShapePaints.solid(m.color, HIGHLIGHT))
    }

    private fun spotlight(canvas: Canvas, m: ShapeMark, g: ShapeGeo) {
        if (g.bw < 1f || g.bh < 1f) return
        val hole = Path()
        if (m.kind == ShapeKind.Spotlight) hole.addCircle(g.cx, g.cy, g.radius, Path.Direction.CW) else hole.addPath(shapePath(m.kind, g))
        val dim = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(0f, 0f, g.w, g.h, Path.Direction.CW)
            addPath(hole)
        }
        canvas.drawPath(dim, ShapePaints.solid(0xFF000000.toInt(), DIM))
        canvas.drawPath(hole, ShapePaints.stroke(m, g.sw))
    }

    private fun lensPaint(source: Bitmap, g: ShapeGeo): Paint {
        val scale = ZOOM * g.w / source.width
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(Matrix().apply {
            postTranslate(-g.cx / g.w * source.width, -g.cy / g.h * source.height)
            postScale(scale, scale)
            postTranslate(g.cx, g.cy)
        })
        return Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
    }

    private fun magnifier(canvas: Canvas, m: ShapeMark, g: ShapeGeo, source: Bitmap?) {
        val rad = g.radius
        if (rad < 2f) return
        val usable = source != null && source.width > 0 && source.height > 0
        val fill = if (usable) lensPaint(source!!, g) else ShapePaints.solid(0xFF1D2633.toInt())
        canvas.drawCircle(g.cx, g.cy, rad, fill)
        canvas.drawCircle(g.cx, g.cy, rad, ShapePaints.stroke(m, g.sw))
        val k = 0.7071f
        val reach = 1.55f
        canvas.drawLine(g.cx + rad * k, g.cy + rad * k, g.cx + rad * reach * k, g.cy + rad * reach * k, ShapePaints.stroke(m, g.sw * 1.9f, dash = false))
    }

    private fun number(canvas: Canvas, m: ShapeMark, g: ShapeGeo) {
        val rad = g.radius
        if (rad < 2f) return
        canvas.drawCircle(g.cx, g.cy, rad, ShapePaints.solid(m.color))
        val ink = ShapePaints.onColor(m.color)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1.5f, rad * 0.1f)
            color = ink
        }
        canvas.drawCircle(g.cx, g.cy, rad, ring)
        val text = m.number.toString()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = rad * if (text.length > 1) 0.95f else 1.2f
        }
        canvas.drawText(text, g.cx, g.cy - (paint.ascent() + paint.descent()) / 2f, paint)
    }

    private fun callout(canvas: Canvas, m: ShapeMark, g: ShapeGeo) {
        if (g.bw < 1f || g.bh < 1f) return
        val tail = polygon(g.l + g.bw * 0.16f, g.b - g.sw, g.l + g.bw * 0.40f, g.b - g.sw, g.l + g.bw * 0.14f, g.b + g.bh * 0.38f)
        val whole = Path(shapePath(ShapeKind.Callout, g))
        whole.op(tail, Path.Op.UNION)
        val f = ShapePaints.fillFactor(m)
        if (f > 0f) canvas.drawPath(whole, ShapePaints.solid(m.color, f))
        canvas.drawPath(whole, ShapePaints.stroke(m, g.sw))
    }

    private fun badge(canvas: Canvas, m: ShapeMark, g: ShapeGeo) {
        val s = min(g.bw, g.bh)
        if (s < 2f) return
        val f = ShapePaints.fillFactor(m)
        if (f > 0f) canvas.drawCircle(g.cx, g.cy, s / 2f, ShapePaints.solid(m.color, f))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (m.fill == ShapeFill.Solid) ShapePaints.onColor(m.color) else m.color
            strokeWidth = max(g.sw, s * 0.1f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path()
        if (m.kind == ShapeKind.Check) {
            path.moveTo(g.cx - s * 0.24f, g.cy + s * 0.02f)
            path.lineTo(g.cx - s * 0.06f, g.cy + s * 0.2f)
            path.lineTo(g.cx + s * 0.26f, g.cy - s * 0.17f)
        } else {
            path.moveTo(g.cx - s * 0.2f, g.cy - s * 0.2f)
            path.lineTo(g.cx + s * 0.2f, g.cy + s * 0.2f)
            path.moveTo(g.cx + s * 0.2f, g.cy - s * 0.2f)
            path.lineTo(g.cx - s * 0.2f, g.cy + s * 0.2f)
        }
        canvas.drawPath(path, paint)
    }
}
