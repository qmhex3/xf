package com.frameout.core.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.min

object MarkRenderer {
    fun draw(source: Bitmap, marks: List<EditMark>, referenceMin: Float = 0f): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(source, 0f, 0f, null)
        marks.forEach { mark ->
            when (mark) {
                is StrokeMark -> drawStroke(canvas, out, mark)
                is CoverMark -> CoverPainter.paint(canvas, out, mark)
                is BrushCoverMark -> CoverPainter.paint(canvas, out, mark, referenceMin)
                is TextMark -> TextPainter.draw(canvas, mark, out.width.toFloat(), out.height.toFloat())
                is ShapeMark -> drawShape(canvas, out, mark)
            }
        }
        return out
    }

    private fun drawShape(canvas: Canvas, bitmap: Bitmap, mark: ShapeMark) {
        val snapshot = if (mark.kind == ShapeKind.Magnifier) bitmap.copy(Bitmap.Config.ARGB_8888, false) else null
        ShapePainter.paint(canvas, mark, bitmap.width.toFloat(), bitmap.height.toFloat(), snapshot, bitmap.width.toFloat())
        snapshot?.recycle()
    }

    private fun drawStroke(canvas: Canvas, bitmap: Bitmap, mark: StrokeMark) {
        if (mark.points.isEmpty()) return
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mark.color
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = (mark.width * min(w, h)).coerceAtLeast(1f)
        }
        canvas.drawPath(smoothPath(mark.points, w, h), paint)
    }

    private fun smoothPath(points: List<MarkPoint>, w: Float, h: Float): Path {
        val first = points.first()
        val path = Path().apply { moveTo(first.x * w, first.y * h) }
        if (points.size == 1) {
            path.lineTo(first.x * w + 0.01f, first.y * h)
            return path
        }
        var prev = first
        points.drop(1).forEach { p ->
            path.quadTo(prev.x * w, prev.y * h, (prev.x + p.x) / 2f * w, (prev.y + p.y) / 2f * h)
            prev = p
        }
        path.lineTo(prev.x * w, prev.y * h)
        return path
    }
}
