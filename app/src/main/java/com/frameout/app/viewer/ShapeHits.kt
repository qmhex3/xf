package com.frameout.app.viewer

import androidx.compose.ui.geometry.Offset
import com.frameout.core.edit.MarkPoint
import com.frameout.core.edit.ShapeKind
import com.frameout.core.edit.ShapeMark
import com.frameout.core.edit.Transform
import com.frameout.core.edit.ViewGeometry
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private fun toNorm(p: Offset, geo: ViewGeometry, t: Transform) = MarkPoint(
    geo.toNormX(p.x - geo.viewW / 2f, t).coerceIn(0f, 1f),
    geo.toNormY(p.y - geo.viewH / 2f, t).coerceIn(0f, 1f)
)

internal fun shapeScreen(x: Float, y: Float, geo: ViewGeometry, t: Transform) =
    Offset(geo.toScreenX(x, t) + geo.viewW / 2f, geo.toScreenY(y, t) + geo.viewH / 2f)

private fun styled(input: ShapeOverlayInput, a: MarkPoint, b: MarkPoint): ShapeMark {
    val d = input.draft
    return ShapeMark(
        kind = d.shapeKind, color = d.color, width = d.shapeWidth,
        x1 = a.x, y1 = a.y, x2 = b.x, y2 = b.y,
        fill = d.shapeFill, dash = d.shapeDash, opacity = d.shapeOpacity, number = nextNumber(d)
    )
}

internal fun buildShape(start: Offset, p: Offset, input: ShapeOverlayInput): ShapeMark? {
    val geo = input.geo
    val t = input.transformNow()
    val kind = input.draft.shapeKind
    val (a, b) = if (kind.radial) {
        val r = max(abs(p.x - start.x), abs(p.y - start.y))
        toNorm(Offset(start.x - r, start.y - r), geo, t) to toNorm(Offset(start.x + r, start.y + r), geo, t)
    } else {
        toNorm(start, geo, t) to toNorm(p, geo, t)
    }
    val big = hypot((b.x - a.x) * geo.imageW, (b.y - a.y) * geo.imageH) >= MIN_EXTENT * min(geo.imageW, geo.imageH)
    return if (big) styled(input, a, b) else null
}

internal fun markerAt(at: Offset, input: ShapeOverlayInput): ShapeMark {
    val geo = input.geo
    val t = input.transformNow()
    val r = ShapeMark.MARKER_SIZE / 2f * min(geo.imageW, geo.imageH) * geo.pixelSize(t)
    return styled(input, toNorm(Offset(at.x - r, at.y - r), geo, t), toNorm(Offset(at.x + r, at.y + r), geo, t))
}

internal fun shiftShape(index: Int, delta: Offset, input: ShapeOverlayInput) {
    val geo = input.geo
    val t = input.transformNow()
    val m = input.draft.marks.getOrNull(index) as? ShapeMark ?: return
    val unitX = geo.imageW * geo.pixelSize(t)
    val unitY = geo.imageH * geo.pixelSize(t)
    val dx = (delta.x / unitX).coerceIn(-min(m.x1, m.x2), 1f - max(m.x1, m.x2))
    val dy = (delta.y / unitY).coerceIn(-min(m.y1, m.y2), 1f - max(m.y1, m.y2))
    input.tools.shapes.replace(index, m.copy(x1 = m.x1 + dx, y1 = m.y1 + dy, x2 = m.x2 + dx, y2 = m.y2 + dy))
}

internal fun dragHandle(grab: Grab.Handle, p: Offset, input: ShapeOverlayInput) {
    val m = input.draft.marks.getOrNull(grab.index) as? ShapeMark ?: return
    val n = toNorm(p, input.geo, input.transformNow())
    input.tools.shapes.replace(grab.index, if (grab.end == 0) m.copy(x1 = n.x, y1 = n.y) else m.copy(x2 = n.x, y2 = n.y))
}

internal fun pickGrab(input: ShapeOverlayInput, at: Offset, density: Float): Grab {
    val geo = input.geo
    val t = input.transformNow()
    val d = input.draft
    val sel = d.shapeSelection
    if (sel != null) {
        val a = shapeScreen(sel.x1, sel.y1, geo, t)
        val b = shapeScreen(sel.x2, sel.y2, geo, t)
        if ((at - deleteAnchor(a, b, geo, density)).getDistance() <= DELETE_RADIUS_DP * density * 1.4f) return Grab.Delete
        val reach = HANDLE_REACH_DP * density
        val da = (at - a).getDistance()
        val db = (at - b).getDistance()
        if (min(da, db) <= reach) return Grab.Handle(d.selectedShape, if (da <= db) 0 else 1)
    }
    for (i in d.marks.indices.reversed()) {
        val m = d.marks[i] as? ShapeMark ?: continue
        if (hits(m, at, geo, t, HIT_TOLERANCE_DP * density)) return Grab.Move(i)
    }
    return Grab.Create
}

private fun hits(m: ShapeMark, p: Offset, geo: ViewGeometry, t: Transform, tol: Float): Boolean {
    val a = shapeScreen(m.x1, m.y1, geo, t)
    val b = shapeScreen(m.x2, m.y2, geo, t)
    if (m.kind.pointBased) return distanceToSegment(p, a, b) <= tol
    val l = min(a.x, b.x)
    val tp = min(a.y, b.y)
    val r = max(a.x, b.x)
    val bt = max(a.y, b.y)
    val inside = p.x in (l - tol)..(r + tol) && p.y in (tp - tol)..(bt + tol)
    if (!inside) return false
    val solid = m.kind.marker || m.kind == ShapeKind.Highlighter || m.kind == ShapeKind.Callout ||
        m.kind == ShapeKind.Magnifier || m.fill != com.frameout.core.edit.ShapeFill.None
    if (solid) return true
    val core = p.x in (l + tol)..(r - tol) && p.y in (tp + tol)..(bt - tol)
    return !core
}

internal fun deleteAnchor(a: Offset, b: Offset, geo: ViewGeometry, density: Float): Offset {
    val r = DELETE_RADIUS_DP * density
    val x = ((a.x + b.x) / 2f).coerceIn(r, max(r, geo.viewW - r))
    val y = (min(a.y, b.y) - DELETE_GAP_DP * density - r).coerceIn(r, max(r, geo.viewH - r))
    return Offset(x, y)
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val len2 = ab.x * ab.x + ab.y * ab.y
    if (len2 < 1e-3f) return (p - a).getDistance()
    val k = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0f, 1f)
    return (p - Offset(a.x + ab.x * k, a.y + ab.y * k)).getDistance()
}
