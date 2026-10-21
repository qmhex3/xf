package com.frameout.app.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.frameout.core.edit.MarkPoint
import com.frameout.core.edit.ShapeKind
import com.frameout.core.edit.ShapeMark
import com.frameout.core.edit.ShapePainter
import com.frameout.core.edit.Transform
import com.frameout.core.edit.ViewGeometry
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private const val HANDLE_RADIUS_DP = 9f
private const val DELETE_RADIUS_DP = 13f
private const val DELETE_GAP_DP = 14f
private val DELETE_RED = Color(0xFFFF3B30)
private const val HANDLE_REACH_DP = 28f
private const val HIT_TOLERANCE_DP = 20f
private const val SLOP_DP = 8f
private const val MIN_EXTENT = 0.012f

internal class ShapeOverlayInput(
    val geo: ViewGeometry,
    val bitmap: Bitmap,
    val draft: EditDraft,
    val transformNow: () -> Transform,
    val tools: ViewerToolEditor,
    val onTransform: (Transform) -> Unit
)

private sealed interface Grab {
    object Create : Grab
    object Delete : Grab
    class Move(val index: Int) : Grab
    class Handle(val index: Int, val end: Int) : Grab
}

@Composable
internal fun ShapeOverlay(input: ShapeOverlayInput, transform: Transform, modifier: Modifier = Modifier) {
    val latest = rememberUpdatedState(input)
    val live = remember { mutableStateOf<ShapeMark?>(null) }
    val density = LocalDensity.current.density
    val accent = FrameTheme.colors.primary
    Canvas(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = EditStrings.drawArea() }
            .pointerInput(Unit) { awaitEachGesture { shapeGesture(latest, live, density) } }
    ) { drawShapes(input, transform, live.value, accent, density) }
}

private suspend fun AwaitPointerEventScope.shapeGesture(latest: State<ShapeOverlayInput>, live: androidx.compose.runtime.MutableState<ShapeMark?>, density: Float) {
    val down = awaitFirstDown(requireUnconsumed = false)
    val start = down.position
    val grab = pickGrab(latest.value, start, density)
    if (grab is Grab.Move) latest.value.tools.shapes.select(grab.index)
    if (grab is Grab.Handle) latest.value.tools.shapes.select(grab.index)
    var multi = false
    var moved = false
    var last = start
    do {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.size >= 2) {
            if (!multi) live.value = null
            multi = true
            pinch(event, latest.value)
        } else if (!multi && pressed.size == 1) {
            val p = pressed[0].position
            if (!moved && (p - start).getDistance() > SLOP_DP * density) {
                moved = true
                last = p
            } else if (moved) {
                when (grab) {
                    Grab.Create -> live.value = build(start, p, latest.value)
                    Grab.Delete -> Unit
                    is Grab.Move -> shift(grab.index, p - last, latest.value)
                    is Grab.Handle -> dragHandle(grab, p, latest.value)
                }
                last = p
            }
        }
        event.changes.forEach { if (it.positionChanged()) it.consume() }
    } while (event.changes.any { it.pressed })
    val input = latest.value
    val preview = live.value
    live.value = null
    if (grab === Grab.Delete && !multi && !moved) input.tools.shapes.deleteSelected()
    if (multi || grab !is Grab.Create) return
    when {
        moved && preview != null -> input.tools.shapes.add(preview)
        !moved && input.draft.shapeKind.marker -> input.tools.shapes.add(markerAt(start, input))
        !moved -> input.tools.shapes.select(-1)
    }
}

private fun pinch(event: PointerEvent, input: ShapeOverlayInput) {
    val centroid = event.calculateCentroid(useCurrent = true)
    val pan = event.calculatePan()
    input.onTransform(
        input.geo.gesture(
            input.transformNow(), event.calculateZoom(),
            centroid.x - input.geo.viewW / 2f, centroid.y - input.geo.viewH / 2f, pan.x, pan.y
        )
    )
}

private fun toNorm(p: Offset, geo: ViewGeometry, t: Transform) = MarkPoint(
    geo.toNormX(p.x - geo.viewW / 2f, t).coerceIn(0f, 1f),
    geo.toNormY(p.y - geo.viewH / 2f, t).coerceIn(0f, 1f)
)

private fun screen(x: Float, y: Float, geo: ViewGeometry, t: Transform) =
    Offset(geo.toScreenX(x, t) + geo.viewW / 2f, geo.toScreenY(y, t) + geo.viewH / 2f)

private fun styled(input: ShapeOverlayInput, a: MarkPoint, b: MarkPoint): ShapeMark {
    val d = input.draft
    return ShapeMark(
        kind = d.shapeKind, color = d.color, width = d.shapeWidth,
        x1 = a.x, y1 = a.y, x2 = b.x, y2 = b.y,
        fill = d.shapeFill, dash = d.shapeDash, opacity = d.shapeOpacity, number = nextNumber(d)
    )
}

private fun build(start: Offset, p: Offset, input: ShapeOverlayInput): ShapeMark? {
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

private fun markerAt(at: Offset, input: ShapeOverlayInput): ShapeMark {
    val geo = input.geo
    val t = input.transformNow()
    val r = ShapeMark.MARKER_SIZE / 2f * min(geo.imageW, geo.imageH) * geo.pixelSize(t)
    return styled(input, toNorm(Offset(at.x - r, at.y - r), geo, t), toNorm(Offset(at.x + r, at.y + r), geo, t))
}

private fun shift(index: Int, delta: Offset, input: ShapeOverlayInput) {
    val geo = input.geo
    val t = input.transformNow()
    val m = input.draft.marks.getOrNull(index) as? ShapeMark ?: return
    val unitX = geo.imageW * geo.pixelSize(t)
    val unitY = geo.imageH * geo.pixelSize(t)
    val dx = (delta.x / unitX).coerceIn(-min(m.x1, m.x2), 1f - max(m.x1, m.x2))
    val dy = (delta.y / unitY).coerceIn(-min(m.y1, m.y2), 1f - max(m.y1, m.y2))
    input.tools.shapes.replace(index, m.copy(x1 = m.x1 + dx, y1 = m.y1 + dy, x2 = m.x2 + dx, y2 = m.y2 + dy))
}

private fun dragHandle(grab: Grab.Handle, p: Offset, input: ShapeOverlayInput) {
    val m = input.draft.marks.getOrNull(grab.index) as? ShapeMark ?: return
    val n = toNorm(p, input.geo, input.transformNow())
    input.tools.shapes.replace(grab.index, if (grab.end == 0) m.copy(x1 = n.x, y1 = n.y) else m.copy(x2 = n.x, y2 = n.y))
}

private fun pickGrab(input: ShapeOverlayInput, at: Offset, density: Float): Grab {
    val geo = input.geo
    val t = input.transformNow()
    val d = input.draft
    val sel = d.shapeSelection
    if (sel != null) {
        val a = screen(sel.x1, sel.y1, geo, t)
        val b = screen(sel.x2, sel.y2, geo, t)
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
    val a = screen(m.x1, m.y1, geo, t)
    val b = screen(m.x2, m.y2, geo, t)
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

private fun deleteAnchor(a: Offset, b: Offset, geo: ViewGeometry, density: Float): Offset {
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

private fun DrawScope.drawShapes(input: ShapeOverlayInput, t: Transform, live: ShapeMark?, accent: Color, density: Float) {
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
    val a = screen(sel.x1, sel.y1, geo, t)
    val b = screen(sel.x2, sel.y2, geo, t)
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
