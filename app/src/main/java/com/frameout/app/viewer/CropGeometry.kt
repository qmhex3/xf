package com.frameout.app.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.hypot

internal enum class CropHandle { None, TopLeft, TopRight, BottomLeft, BottomRight, Top, Bottom, Left, Right, Move }

internal data class CropBox(val l: Float, val t: Float, val r: Float, val b: Float)

internal const val CROP_MIN_SIDE = 0.06f

internal fun fitRect(area: Size, aspect: Float): Rect {
    if (area.width <= 0f || area.height <= 0f || aspect <= 0f) return Rect(Offset.Zero, area)
    val w = if (area.width / area.height > aspect) area.height * aspect else area.width
    val h = w / aspect
    return Rect(Offset((area.width - w) / 2f, (area.height - h) / 2f), Size(w, h))
}

internal fun CropBox.toRect(img: Rect) = Rect(
    img.left + l * img.width, img.top + t * img.height,
    img.left + r * img.width, img.top + b * img.height
)

internal fun hitCrop(pos: Offset, img: Rect, box: CropBox, cornerPx: Float, edgePx: Float): CropHandle {
    val c = box.toRect(img)
    val corners = listOf(
        CropHandle.TopLeft to c.topLeft, CropHandle.TopRight to c.topRight,
        CropHandle.BottomLeft to c.bottomLeft, CropHandle.BottomRight to c.bottomRight
    )
    val corner = corners.minByOrNull { hypot(pos.x - it.second.x, pos.y - it.second.y) }
    if (corner != null && hypot(pos.x - corner.second.x, pos.y - corner.second.y) <= cornerPx) return corner.first
    val spanX = pos.x in (c.left - edgePx)..(c.right + edgePx)
    val spanY = pos.y in (c.top - edgePx)..(c.bottom + edgePx)
    val edges = buildList {
        if (spanX) { add(CropHandle.Top to abs(pos.y - c.top)); add(CropHandle.Bottom to abs(pos.y - c.bottom)) }
        if (spanY) { add(CropHandle.Left to abs(pos.x - c.left)); add(CropHandle.Right to abs(pos.x - c.right)) }
    }
    val edge = edges.minByOrNull { it.second }
    if (edge != null && edge.second <= edgePx) return edge.first
    return if (c.contains(pos)) CropHandle.Move else CropHandle.None
}

internal fun dragCrop(handle: CropHandle, s: CropBox, dx: Float, dy: Float): CropBox {
    if (handle == CropHandle.Move) {
        val l = (s.l + dx).coerceIn(0f, 1f - (s.r - s.l))
        val t = (s.t + dy).coerceIn(0f, 1f - (s.b - s.t))
        return CropBox(l, t, l + (s.r - s.l), t + (s.b - s.t))
    }
    val left = handle == CropHandle.TopLeft || handle == CropHandle.BottomLeft || handle == CropHandle.Left
    val right = handle == CropHandle.TopRight || handle == CropHandle.BottomRight || handle == CropHandle.Right
    val top = handle == CropHandle.TopLeft || handle == CropHandle.TopRight || handle == CropHandle.Top
    val bottom = handle == CropHandle.BottomLeft || handle == CropHandle.BottomRight || handle == CropHandle.Bottom
    return CropBox(
        if (left) (s.l + dx).coerceIn(0f, s.r - CROP_MIN_SIDE) else s.l,
        if (top) (s.t + dy).coerceIn(0f, s.b - CROP_MIN_SIDE) else s.t,
        if (right) (s.r + dx).coerceIn(s.l + CROP_MIN_SIDE, 1f) else s.r,
        if (bottom) (s.b + dy).coerceIn(s.t + CROP_MIN_SIDE, 1f) else s.b
    )
}
