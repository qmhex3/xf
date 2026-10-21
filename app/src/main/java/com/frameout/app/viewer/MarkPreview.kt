package com.frameout.app.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.frameout.core.edit.BrushCoverMark
import com.frameout.core.edit.CoverMark
import com.frameout.core.edit.CoverStyle
import com.frameout.core.edit.EditMark
import com.frameout.core.edit.MarkPoint
import com.frameout.core.edit.StrokeMark
import com.frameout.core.edit.Transform
import com.frameout.core.edit.ViewGeometry
import kotlin.math.max
import kotlin.math.min

private const val MIN_PREVIEW_WIDTH = 1f
private const val RING_MIN_RADIUS = 3f

internal fun DrawScope.drawMarkPreview(mark: EditMark, geo: ViewGeometry, t: Transform) {
    when (mark) {
        is StrokeMark -> drawStrokePreview(mark, geo, t)
        is BrushCoverMark -> drawBrushPreview(mark, geo, t)
        is CoverMark -> drawRectPreview(mark, geo, t)
        is com.frameout.core.edit.TextMark -> Unit
        is com.frameout.core.edit.ShapeMark -> Unit
    }
}

private fun widthOnScreen(fraction: Float, geo: ViewGeometry, t: Transform): Float =
    max(MIN_PREVIEW_WIDTH, fraction * min(geo.imageW, geo.imageH) * geo.pixelSize(t))

private fun screenPoints(points: List<MarkPoint>, geo: ViewGeometry, t: Transform): List<Offset> {
    val cx = geo.viewW / 2f
    val cy = geo.viewH / 2f
    return points.map { Offset(geo.toScreenX(it.x, t) + cx, geo.toScreenY(it.y, t) + cy) }
}

private fun DrawScope.drawStrokePreview(mark: StrokeMark, geo: ViewGeometry, t: Transform) {
    val pts = screenPoints(mark.points, geo, t)
    val stroke = Stroke(widthOnScreen(mark.width, geo, t), cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(smoothPath(pts), Color(mark.color), style = stroke)
}

private fun DrawScope.drawBrushPreview(mark: BrushCoverMark, geo: ViewGeometry, t: Transform) {
    val pts = screenPoints(mark.points, geo, t)
    val stroke = Stroke(widthOnScreen(mark.width, geo, t), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val color = if (mark.style == CoverStyle.Solid) Color.Black else Color.White.copy(alpha = 0.32f)
    drawPath(linePath(pts), color, style = stroke)
}

private fun DrawScope.drawRectPreview(mark: CoverMark, geo: ViewGeometry, t: Transform) {
    val corners = screenPoints(listOf(MarkPoint(mark.left, mark.top), MarkPoint(mark.right, mark.bottom)), geo, t)
    val topLeft = corners[0]
    val box = Size(corners[1].x - corners[0].x, corners[1].y - corners[0].y)
    if (mark.style == CoverStyle.Solid) {
        drawRect(Color.Black, topLeft, box)
        return
    }
    drawRect(Color.White.copy(alpha = 0.18f), topLeft, box)
    drawRect(Color.White, topLeft, box, style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))))
}

internal fun DrawScope.drawBrushRing(center: Offset, diameter: Float) {
    val radius = max(RING_MIN_RADIUS, diameter / 2f)
    drawCircle(Color.Black.copy(alpha = 0.55f), radius + 1f, center, style = Stroke(2f))
    drawCircle(Color.White.copy(alpha = 0.95f), radius, center, style = Stroke(1.5f))
}

internal fun brushDiameterOnScreen(fraction: Float, geo: ViewGeometry, t: Transform): Float = widthOnScreen(fraction, geo, t)

private fun linePath(pts: List<Offset>): Path {
    val path = Path()
    val first = pts.first()
    path.moveTo(first.x, first.y)
    if (pts.size == 1) path.lineTo(first.x + 0.01f, first.y)
    pts.drop(1).forEach { path.lineTo(it.x, it.y) }
    return path
}

private fun smoothPath(pts: List<Offset>): Path {
    val first = pts.first()
    val path = Path()
    path.moveTo(first.x, first.y)
    if (pts.size == 1) {
        path.lineTo(first.x + 0.01f, first.y)
        return path
    }
    var prev = first
    pts.drop(1).forEach { p ->
        path.quadraticTo(prev.x, prev.y, (prev.x + p.x) / 2f, (prev.y + p.y) / 2f)
        prev = p
    }
    path.lineTo(prev.x, prev.y)
    return path
}
