package com.frameout.app.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.frameout.core.edit.ShapeMark
import com.frameout.core.edit.ShapePainter
import com.frameout.core.edit.Transform
import kotlin.math.abs
import kotlin.math.min

internal fun DrawScope.drawShapes(input: ShapeOverlayInput, t: Transform, live: ShapeMark?, accent: Color, density: Float) {
    val geo = input.geo
    val ox = geo.toScreenX(0f, t) + geo.viewW / 2f
    val oy = geo.toScreenY(0f, t) + geo.viewH / 2f
    val pw = geo.imageW * geo.pixelSize(t)
    val ph = geo.imageH * geo.pixelSize(t)
    val shapes = input.draft.marks.filterIsInstance<ShapeMark>() + listOfNotNull(live)
    drawIntoCanvas { c ->
        val native = c.nativeCanvas
        native.save()
        native.translate(ox, oy)
        native.clipRect(0f, 0f, pw, ph)
        shapes.forEach { ShapePainter.paint(native, it, pw, ph, input.bitmap, input.bitmap.width.toFloat()) }
        native.restore()
    }
    val sel = (live ?: input.draft.shapeSelection) ?: return
    val a = shapeScreen(sel.x1, sel.y1, geo, t)
    val b = shapeScreen(sel.x2, sel.y2, geo, t)
    if (!sel.kind.pointBased) {
        val topLeft = Offset(min(a.x, b.x), min(a.y, b.y))
        val size = Size(abs(b.x - a.x), abs(b.y - a.y))
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f * density, 7f * density))
        drawRect(Color.Black.copy(alpha = 0.45f), topLeft, size, style = Stroke(2.6f * density, pathEffect = dash))
        drawRect(Color.White.copy(alpha = 0.95f), topLeft, size, style = Stroke(1.4f * density, pathEffect = dash))
    }
    if (live != null) return
    listOf(a, b).forEach { p ->
        drawCircle(Color.White, HANDLE_RADIUS_DP * density, p)
        drawCircle(accent, HANDLE_RADIUS_DP * density, p, style = Stroke(2.5f * density))
    }
    drawDeleteHandle(deleteAnchor(a, b, input.geo, density), density)
}

private fun DrawScope.drawDeleteHandle(at: Offset, density: Float) {
    val r = DELETE_RADIUS_DP * density
    drawHandle(at, r, DELETE_RED) { drawCross(it, r * 0.42f) }
}
