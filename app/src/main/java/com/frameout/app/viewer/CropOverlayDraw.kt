package com.frameout.app.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawCropDim(img: Rect, c: Rect) {
    val dim = Color.Black.copy(alpha = 0.58f)
    fun fill(l: Float, t: Float, r: Float, b: Float) {
        if (r > l && b > t) drawRect(dim, Offset(l, t), androidx.compose.ui.geometry.Size(r - l, b - t))
    }
    fill(img.left, img.top, img.right, c.top)
    fill(img.left, c.bottom, img.right, img.bottom)
    fill(img.left, c.top, c.left, c.bottom)
    fill(c.right, c.top, img.right, c.bottom)
}

internal fun DrawScope.drawCropFrame(c: Rect, gridAlpha: Float) {
    if (gridAlpha > 0.01f) {
        val line = Color.White.copy(alpha = 0.38f * gridAlpha)
        for (i in 1..2) {
            val x = c.left + c.width * i / 3f
            val y = c.top + c.height * i / 3f
            drawLine(line, Offset(x, c.top), Offset(x, c.bottom), 1.dp.toPx())
            drawLine(line, Offset(c.left, y), Offset(c.right, y), 1.dp.toPx())
        }
    }
    drawRect(Color.White.copy(alpha = 0.92f), c.topLeft, c.size, style = Stroke(1.5.dp.toPx()))
}

internal fun DrawScope.drawCropHandles(c: Rect, accent: Color, active: CropHandle, scale: Float) {
    val arm = 22.dp.toPx() * scale
    val w = 4.dp.toPx()
    val shadow = Color.Black.copy(alpha = 0.35f)
    fun bracket(corner: Offset, sx: Float, sy: Float, on: Boolean) {
        val color = if (on) accent else Color.White
        val h = Offset(corner.x + sx * arm, corner.y)
        val v = Offset(corner.x, corner.y + sy * arm)
        drawLine(shadow, corner + Offset(0f, 1.5.dp.toPx()), h + Offset(0f, 1.5.dp.toPx()), w + 1.dp.toPx(), StrokeCap.Round)
        drawLine(color, corner, h, w, StrokeCap.Round)
        drawLine(color, corner, v, w, StrokeCap.Round)
    }
    bracket(c.topLeft, 1f, 1f, active == CropHandle.TopLeft)
    bracket(c.topRight, -1f, 1f, active == CropHandle.TopRight)
    bracket(c.bottomLeft, 1f, -1f, active == CropHandle.BottomLeft)
    bracket(c.bottomRight, -1f, -1f, active == CropHandle.BottomRight)
    val half = 14.dp.toPx() * scale
    fun bar(a: Offset, b: Offset, on: Boolean) = drawLine(if (on) accent else Color.White, a, b, w, StrokeCap.Round)
    bar(Offset(c.center.x - half, c.top), Offset(c.center.x + half, c.top), active == CropHandle.Top)
    bar(Offset(c.center.x - half, c.bottom), Offset(c.center.x + half, c.bottom), active == CropHandle.Bottom)
    bar(Offset(c.left, c.center.y - half), Offset(c.left, c.center.y + half), active == CropHandle.Left)
    bar(Offset(c.right, c.center.y - half), Offset(c.right, c.center.y + half), active == CropHandle.Right)
}
