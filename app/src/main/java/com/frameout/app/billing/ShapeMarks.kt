package com.frameout.app.billing

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

internal class Mark(val path: Path, val color: Color, val start: Float, val span: Float, val width: Float)

internal fun buildMarks(s: Size, a: Color, b: Color, c: Color): List<Mark> {
    val w = s.width
    val h = s.height
    val shaft = Path().apply {
        moveTo(w * 0.42f, h * 0.20f)
        cubicTo(w * 0.52f, h * 0.06f, w * 0.64f, h * 0.10f, w * 0.71f, h * 0.43f)
    }
    val head = Path().apply {
        moveTo(w * 0.64f, h * 0.35f)
        lineTo(w * 0.71f, h * 0.45f)
        lineTo(w * 0.78f, h * 0.34f)
    }
    val face = Offset(w * SceneZones.face.x, h * SceneZones.face.y)
    val ring = Path().apply { addOval(Rect(face, h * 0.20f)) }
    val box = Path().apply { addRoundRect(RoundRect(plateRect(s), CornerRadius(10f, 10f))) }
    return listOf(Mark(shaft, a, 0f, 0.30f, 3.4f), Mark(head, a, 0.26f, 0.08f, 3.4f), Mark(ring, b, 0.20f, 0.30f, 3f), Mark(box, c, 0.42f, 0.30f, 3f))
}

internal fun plateRect(s: Size): Rect = Rect(s.width * 0.53f, s.height * 0.64f, s.width * 0.92f, s.height * 0.86f)

private fun Path.partial(fraction: Float): Path {
    val measure = PathMeasure()
    measure.setPath(this, false)
    val out = Path()
    measure.getSegment(0f, measure.length * fraction, out, true)
    return out
}

internal fun DrawScope.drawMark(m: Mark, progress: Float, fade: Float) {
    val local = ((progress - m.start) / m.span).coerceIn(0f, 1f)
    if (local <= 0f) return
    val visible = m.path.partial(local)
    val width = m.width.dp.toPx()
    drawPath(visible, m.color.copy(alpha = 0.22f * fade), style = Stroke(width * 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(visible, m.color.copy(alpha = fade), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

internal fun DrawScope.drawBadge(color: Color, progress: Float, fade: Float) {
    val k = ((progress - 0.62f) / 0.10f).coerceIn(0f, 1f) * fade
    if (k <= 0f) return
    val c = Offset(size.width * 0.13f, size.height * 0.25f)
    drawCircle(color.copy(alpha = 0.25f * k), 15.dp.toPx() * k, c)
    drawCircle(color, 10.dp.toPx() * k, c)
    drawCircle(Color.White.copy(alpha = k), 3.dp.toPx() * k, c)
}

internal fun DrawScope.drawHandles(color: Color, progress: Float, fade: Float) {
    val k = ((progress - 0.72f) / 0.08f).coerceIn(0f, 1f) * fade
    if (k <= 0f) return
    val r = plateRect(size)
    listOf(r.topLeft, r.topRight, r.bottomLeft, r.bottomRight).forEach {
        drawCircle(Color.White.copy(alpha = k), 4.5.dp.toPx() * k, it)
        drawCircle(color.copy(alpha = k), 4.5.dp.toPx() * k, it, style = Stroke(1.5.dp.toPx()))
    }
}
