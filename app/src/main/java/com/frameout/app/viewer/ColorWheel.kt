package com.frameout.app.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.frameout.core.edit.ColorMath
import com.frameout.core.edit.Hsv
import com.frameout.core.util.EditStrings
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private const val RING_FRACTION = 0.15f
private const val SQUARE_FRACTION = 0.68f

private class WheelGeometry(width: Float, height: Float) {
    val center = Offset(width / 2f, height / 2f)
    val outer = min(width, height) / 2f
    val ringWidth = outer * RING_FRACTION * 2f
    val inner = outer - ringWidth
    val half = inner * SQUARE_FRACTION
    val side = half * 2f
    val left = center.x - half
    val top = center.y - half

    fun onRing(p: Offset): Boolean = hypot(p.x - center.x, p.y - center.y) >= inner - ringWidth * 0.15f

    fun hueAt(p: Offset): Float = ((Math.toDegrees(atan2(p.y - center.y, p.x - center.x).toDouble()).toFloat()) + 360f) % 360f

    fun saturationAt(p: Offset): Float = ((p.x - left) / side).coerceIn(0f, 1f)

    fun valueAt(p: Offset): Float = 1f - ((p.y - top) / side).coerceIn(0f, 1f)
}

@Composable
internal fun ColorWheel(hsv: Hsv, onChange: (Hsv) -> Unit, modifier: Modifier = Modifier) {
    val latest by rememberUpdatedState(hsv)
    val latestChange by rememberUpdatedState(onChange)
    val label = EditStrings.customColor()
    Canvas(
        modifier
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val geo = WheelGeometry(size.width.toFloat(), size.height.toFloat())
                    val ring = geo.onRing(down.position)
                    var position = down.position
                    do {
                        latestChange(wheelUpdate(geo, ring, position, latest))
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        position = event.changes.firstOrNull()?.position ?: position
                    } while (event.changes.any { it.pressed })
                }
            }
    ) { drawWheel(WheelGeometry(size.width, size.height), hsv) }
}

private fun wheelUpdate(geo: WheelGeometry, ring: Boolean, p: Offset, current: Hsv): Hsv =
    if (ring) current.copy(h = geo.hueAt(p)) else current.copy(s = geo.saturationAt(p), v = geo.valueAt(p))

private fun DrawScope.drawWheel(geo: WheelGeometry, hsv: Hsv) {
    val mid = geo.outer - geo.ringWidth / 2f
    drawCircle(Brush.sweepGradient(HUE_COLORS, geo.center), mid, geo.center, style = Stroke(geo.ringWidth))
    val hue = Color(ColorMath.toArgb(Hsv(hsv.h, 1f, 1f)))
    val topLeft = Offset(geo.left, geo.top)
    val box = Size(geo.side, geo.side)
    val corner = CornerRadius(8f, 8f)
    drawRoundRect(Brush.horizontalGradient(listOf(Color.White, hue), geo.left, geo.left + geo.side), topLeft, box, corner)
    drawRoundRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), geo.top, geo.top + geo.side), topLeft, box, corner)
    val angle = Math.toRadians(hsv.h.toDouble())
    val marker = Offset(geo.center.x + (mid * cos(angle)).toFloat(), geo.center.y + (mid * sin(angle)).toFloat())
    drawMarker(marker, hue, geo.ringWidth * 0.36f)
    drawMarker(Offset(geo.left + hsv.s * geo.side, geo.top + (1f - hsv.v) * geo.side), Color(ColorMath.toArgb(hsv)), geo.ringWidth * 0.36f)
}

private fun DrawScope.drawMarker(at: Offset, fill: Color, radius: Float) {
    drawCircle(fill, radius, at)
    drawCircle(Color.Black.copy(alpha = 0.5f), radius + 1.5f, at, style = Stroke(2f))
    drawCircle(Color.White, radius, at, style = Stroke(2.5f))
}
