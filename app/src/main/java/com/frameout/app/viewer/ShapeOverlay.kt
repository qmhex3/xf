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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.frameout.core.edit.ShapeMark
import com.frameout.core.edit.Transform
import com.frameout.core.edit.ViewGeometry
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal const val HANDLE_RADIUS_DP = 9f
internal const val DELETE_RADIUS_DP = 13f
internal const val DELETE_GAP_DP = 14f
internal val DELETE_RED = Color(0xFFFF3B30)
internal const val HANDLE_REACH_DP = 28f
internal const val HIT_TOLERANCE_DP = 20f
private const val SLOP_DP = 8f
internal const val MIN_EXTENT = 0.012f
private const val PINCH_REACH_DP = 36f

internal class ShapeOverlayInput(
    val geo: ViewGeometry,
    val bitmap: Bitmap,
    val draft: EditDraft,
    val transformNow: () -> Transform,
    val tools: ViewerToolEditor,
    val onTransform: (Transform) -> Unit
)

internal sealed interface Grab {
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
    var pinched = -1
    do {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.size >= 2) {
            if (!multi) {
                live.value = null
                pinched = pinchTarget(latest.value, grab, pressed.map { it.position }, density)
            }
            multi = true
            if (pinched >= 0) pinchShape(event, latest.value, pinched) else pinch(event, latest.value)
        } else if (!multi && pressed.size == 1) {
            val p = pressed[0].position
            if (!moved && (p - start).getDistance() > SLOP_DP * density) {
                moved = true
                last = p
            } else if (moved) {
                when (grab) {
                    Grab.Create -> live.value = buildShape(start, p, latest.value)
                    Grab.Delete -> Unit
                    is Grab.Move -> shiftShape(grab.index, p - last, latest.value)
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

private fun pinchTarget(input: ShapeOverlayInput, grab: Grab, fingers: List<Offset>, density: Float): Int {
    when (grab) {
        is Grab.Move -> return grab.index
        is Grab.Handle -> return grab.index
        else -> Unit
    }
    val d = input.draft
    val sel = d.shapeSelection ?: return -1
    val geo = input.geo
    val t = input.transformNow()
    val tol = PINCH_REACH_DP * density
    val a = shapeScreen(sel.x1, sel.y1, geo, t)
    val b = shapeScreen(sel.x2, sel.y2, geo, t)
    val inside = fingers.any { p -> p.x in (min(a.x, b.x) - tol)..(max(a.x, b.x) + tol) && p.y in (min(a.y, b.y) - tol)..(max(a.y, b.y) + tol) }
    return if (inside) d.selectedShape else -1
}

private fun pinchShape(event: PointerEvent, input: ShapeOverlayInput, index: Int) {
    val geo = input.geo
    val t = input.transformNow()
    val m = input.draft.marks.getOrNull(index) as? ShapeMark ?: return
    val pan = event.calculatePan()
    val unitX = geo.imageW * geo.pixelSize(t)
    val unitY = geo.imageH * geo.pixelSize(t)
    val cx = (m.x1 + m.x2) / 2f
    val cy = (m.y1 + m.y2) / 2f
    val w = abs(m.x2 - m.x1)
    val h = abs(m.y2 - m.y1)
    val minSide = min(geo.imageW, geo.imageH).toFloat()
    val diag = hypot(w * geo.imageW, h * geo.imageH)
    val low = (MIN_EXTENT * 2f * minSide) / diag.coerceAtLeast(1f)
    val high = 1f / max(max(w, h), 1e-4f)
    val zoom = event.calculateZoom().coerceIn(low.coerceAtMost(1f), high.coerceAtLeast(1f))
    val nx1 = cx + (m.x1 - cx) * zoom
    val ny1 = cy + (m.y1 - cy) * zoom
    val nx2 = cx + (m.x2 - cx) * zoom
    val ny2 = cy + (m.y2 - cy) * zoom
    val lowX = min(nx1, nx2)
    val highX = max(nx1, nx2)
    val lowY = min(ny1, ny2)
    val highY = max(ny1, ny2)
    val shiftX = if (lowX < 0f) -lowX else if (highX > 1f) 1f - highX else 0f
    val shiftY = if (lowY < 0f) -lowY else if (highY > 1f) 1f - highY else 0f
    val dx = (pan.x / unitX).coerceIn(-(lowX + shiftX), 1f - (highX + shiftX))
    val dy = (pan.y / unitY).coerceIn(-(lowY + shiftY), 1f - (highY + shiftY))
    input.tools.shapes.replace(index, m.copy(x1 = nx1 + shiftX + dx, y1 = ny1 + shiftY + dy, x2 = nx2 + shiftX + dx, y2 = ny2 + shiftY + dy))
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
