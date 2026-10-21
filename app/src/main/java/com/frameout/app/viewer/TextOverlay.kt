package com.frameout.app.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.TextMark
import com.frameout.core.edit.TextPainter
import com.frameout.core.edit.Transform
import com.frameout.core.edit.ViewGeometry
import com.frameout.core.util.EditStrings
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val HANDLE_RADIUS_DP = 13f
private const val HANDLE_HIT_DP = 26f
private const val BODY_SLOP_DP = 10f
private const val TAP_SLOP_DP = 8f

internal class TextOverlayInput(
    val geo: ViewGeometry,
    val bitmap: Bitmap,
    val draft: EditDraft,
    val transformNow: () -> Transform,
    val onTransform: (Transform) -> Unit,
    val tools: ViewerToolEditor
)

private class ScreenBox(val center: Offset, val corners: List<Offset>) {
    val topLeft get() = corners[0]
    val bottomRight get() = corners[2]
}

private fun screenBox(mark: TextMark, geo: ViewGeometry, t: Transform): ScreenBox {
    val size = TextPainter.boxSize(mark, geo.imageW.toFloat(), geo.imageH.toFloat())
    val k = geo.pixelSize(t)
    val hw = size.width * k / 2f
    val hh = size.height * k / 2f
    val center = Offset(geo.toScreenX(mark.x, t) + geo.viewW / 2f, geo.toScreenY(mark.y, t) + geo.viewH / 2f)
    val rad = Math.toRadians(mark.rotation.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    fun at(x: Float, y: Float) = Offset(center.x + x * c - y * s, center.y + x * s + y * c)
    return ScreenBox(center, listOf(at(-hw, -hh), at(hw, -hh), at(hw, hh), at(-hw, hh)))
}

private fun hitBody(box: ScreenBox, mark: TextMark, p: Offset, slop: Float, geo: ViewGeometry, t: Transform): Boolean {
    val size = TextPainter.boxSize(mark, geo.imageW.toFloat(), geo.imageH.toFloat())
    val k = geo.pixelSize(t)
    val rad = Math.toRadians(-mark.rotation.toDouble())
    val dx = p.x - box.center.x
    val dy = p.y - box.center.y
    val lx = dx * cos(rad).toFloat() - dy * sin(rad).toFloat()
    val ly = dx * sin(rad).toFloat() + dy * cos(rad).toFloat()
    return kotlin.math.abs(lx) <= size.width * k / 2f + slop && kotlin.math.abs(ly) <= size.height * k / 2f + slop
}

@Composable
internal fun TextOverlay(input: TextOverlayInput, transform: Transform, modifier: Modifier = Modifier) {
    val latest = rememberUpdatedState(input)
    val focus = LocalFocusManager.current
    val label = EditStrings.textArea()
    Canvas(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = label }
            .pointerInput(Unit) { awaitEachGesture { textGesture(latest) { focus.clearFocus() } } }
    ) {
        drawTexts(input, transform)
        drawSelection(input, transform)
    }
}

private enum class Mode { Delete, Resize, Drag, Background }

private suspend fun AwaitPointerEventScope.textGesture(latest: State<TextOverlayInput>, onDown: () -> Unit) {
    val down = awaitFirstDown(requireUnconsumed = false)
    onDown()
    val start = latest.value
    val geo = start.geo
    val t0 = start.transformNow()
    val handleHit = HANDLE_HIT_DP.dp.toPx()
    val slop = BODY_SLOP_DP.dp.toPx()
    val tapSlop = TAP_SLOP_DP.dp.toPx()
    val selected = start.draft.selectedText
    val selectedBox = selected?.let { screenBox(it, geo, t0) }
    var mode = Mode.Background
    if (selected != null && selectedBox != null) {
        mode = when {
            (down.position - selectedBox.topLeft).getDistance() <= handleHit -> Mode.Delete
            (down.position - selectedBox.bottomRight).getDistance() <= handleHit -> Mode.Resize
            hitBody(selectedBox, selected, down.position, slop, geo, t0) -> Mode.Drag
            else -> Mode.Background
        }
    }
    if (mode == Mode.Background) {
        val hit = start.draft.marks.indices.reversed().firstOrNull { i ->
            val m = start.draft.marks[i] as? TextMark ?: return@firstOrNull false
            !m.blank && hitBody(screenBox(m, geo, t0), m, down.position, slop, geo, t0)
        }
        if (hit != null) {
            start.tools.selectText(hit)
            mode = Mode.Drag
        }
    }
    val center = selectedBox?.center ?: down.position
    val startDist = (down.position - center).getDistance().coerceAtLeast(1f)
    val startAngle = angleOf(down.position, center)
    val startSize = selected?.size ?: 0f
    val startRotation = selected?.rotation ?: 0f
    var moved = false
    var last = down.position
    do {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        val now = latest.value
        if ((pressed.firstOrNull()?.position ?: last).let { (it - down.position).getDistance() } > tapSlop) moved = true
        when {
            pressed.size >= 2 -> {
                moved = true
                if (mode == Mode.Background) pinchImage(event, now) else pinchText(event, now)
            }
            pressed.size == 1 -> {
                val p = pressed[0].position
                when (mode) {
                    Mode.Drag -> dragText(now, p - last)
                    Mode.Resize -> {
                        val dist = (p - center).getDistance()
                        now.tools.setTextGeometry(startSize * dist / startDist, startRotation + (angleOf(p, center) - startAngle))
                    }
                    Mode.Background -> if (moved) panImage(now, p - last)
                    Mode.Delete -> Unit
                }
                last = p
            }
        }
        event.changes.forEach { if (it.positionChanged()) it.consume() }
    } while (event.changes.any { it.pressed })
    if (moved) return
    when (mode) {
        Mode.Delete -> start.tools.deleteSelectedText()
        Mode.Background -> start.tools.deselectText()
        else -> Unit
    }
}

private fun angleOf(p: Offset, center: Offset): Float =
    Math.toDegrees(atan2((p.y - center.y).toDouble(), (p.x - center.x).toDouble())).toFloat()

private fun dragText(input: TextOverlayInput, delta: Offset) {
    val geo = input.geo
    val k = geo.pixelSize(input.transformNow())
    input.tools.moveText(delta.x / (k * geo.imageW), delta.y / (k * geo.imageH))
}

private fun pinchText(event: PointerEvent, input: TextOverlayInput) {
    val zoom = event.calculateZoom()
    val rotation = event.calculateRotation()
    val pan = event.calculatePan()
    dragText(input, pan)
    input.tools.scaleRotateText(zoom, rotation)
}

private fun pinchImage(event: PointerEvent, input: TextOverlayInput) {
    val geo = input.geo
    val centroid = event.calculateCentroid(useCurrent = true)
    val pan = event.calculatePan()
    input.onTransform(
        geo.gesture(input.transformNow(), event.calculateZoom(), centroid.x - geo.viewW / 2f, centroid.y - geo.viewH / 2f, pan.x, pan.y)
    )
}

private fun panImage(input: TextOverlayInput, delta: Offset) {
    val geo = input.geo
    input.onTransform(geo.gesture(input.transformNow(), 1f, 0f, 0f, delta.x, delta.y))
}

private fun DrawScope.drawTexts(input: TextOverlayInput, t: Transform) {
    val geo = input.geo
    val k = geo.pixelSize(t)
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        native.save()
        native.translate(geo.viewW / 2f + t.x, geo.viewH / 2f + t.y)
        native.scale(k, k)
        native.translate(-geo.imageW / 2f, -geo.imageH / 2f)
        input.draft.marks.forEach { mark ->
            if (mark is TextMark) TextPainter.draw(native, mark, geo.imageW.toFloat(), geo.imageH.toFloat())
        }
        native.restore()
    }
}

private fun DrawScope.drawSelection(input: TextOverlayInput, t: Transform) {
    val mark = input.draft.selectedText ?: return
    val box = screenBox(mark, input.geo, t)
    val path = Path().apply {
        moveTo(box.corners[0].x, box.corners[0].y)
        box.corners.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, Color.Black.copy(alpha = 0.45f), style = Stroke(width = 3.5.dp.toPx()))
    drawPath(path, Color.White, style = Stroke(width = 1.5.dp.toPx()))
    val r = HANDLE_RADIUS_DP.dp.toPx()
    drawHandle(box.topLeft, r, Color(0xFFFF3B30)) { drawCross(it, r * 0.42f) }
    drawHandle(box.bottomRight, r, Color(0xFF0A84FF)) { drawResizeArrow(it, r * 0.45f) }
}

internal fun DrawScope.drawHandle(at: Offset, radius: Float, fill: Color, glyph: DrawScope.(Offset) -> Unit) {
    drawCircle(Color.Black.copy(alpha = 0.35f), radius + 2.dp.toPx(), at)
    drawCircle(fill, radius, at)
    drawCircle(Color.White, radius, at, style = Stroke(2.dp.toPx()))
    glyph(at)
}

internal fun DrawScope.drawCross(c: Offset, h: Float) {
    val w = 2.dp.toPx()
    drawLine(Color.White, Offset(c.x - h, c.y - h), Offset(c.x + h, c.y + h), w, StrokeCap.Round)
    drawLine(Color.White, Offset(c.x - h, c.y + h), Offset(c.x + h, c.y - h), w, StrokeCap.Round)
}

private fun DrawScope.drawResizeArrow(c: Offset, h: Float) {
    val w = 2.dp.toPx()
    drawLine(Color.White, Offset(c.x - h, c.y + h), Offset(c.x + h, c.y - h), w, StrokeCap.Round)
    drawLine(Color.White, Offset(c.x + h, c.y - h), Offset(c.x + h * 0.1f, c.y - h), w, StrokeCap.Round)
    drawLine(Color.White, Offset(c.x + h, c.y - h), Offset(c.x + h, c.y - h * 0.1f), w, StrokeCap.Round)
    drawLine(Color.White, Offset(c.x - h, c.y + h), Offset(c.x - h * 0.1f, c.y + h), w, StrokeCap.Round)
    drawLine(Color.White, Offset(c.x - h, c.y + h), Offset(c.x - h, c.y + h * 0.1f), w, StrokeCap.Round)
}
