package com.frameout.app.picker

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.min

internal class FlightPose(val center: Offset, val width: Float, val height: Float, val corner: Float, val lift: Float)

internal class FlightStage(val rect: Rect, val corner: Float)

internal class FlightEnd(val center: Offset, val slot: Float, val corner: Float)

private const val RISE_END = 0.36f
private const val HOLD_END = 0.5f
private const val STAGE_WIDTH_FRACTION = 0.5f
private const val STAGE_HEIGHT_FRACTION = 0.3f

private val RiseEasing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1.1f)
private val TravelEasing = CubicBezierEasing(0.5f, 0f, 0.2f, 1f)

private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun mix(a: Offset, b: Offset, t: Float) = Offset(mix(a.x, b.x, t), mix(a.y, b.y, t))

internal fun flightStage(origin: Offset, layer: Size, from: Rect, maxEdgePx: Float, scale: Float, corner: Float): FlightStage {
    val aspect = if (from.height > 0f) from.width / from.height else 1f
    val edge = min(min(layer.width * STAGE_WIDTH_FRACTION, layer.height * STAGE_HEIGHT_FRACTION), maxEdgePx) * scale
    val w = if (aspect >= 1f) edge else edge * aspect
    val h = if (aspect >= 1f) edge / aspect else edge
    val center = Offset(origin.x + layer.width / 2f, origin.y + layer.height / 2f)
    return FlightStage(Rect(center.x - w / 2f, center.y - h / 2f, center.x + w / 2f, center.y + h / 2f), corner)
}

internal fun flightPose(t: Float, from: Rect, fromCorner: Float, stage: FlightStage, end: FlightEnd): FlightPose {
    val s = stage.rect
    return when {
        t < RISE_END -> {
            val e = RiseEasing.transform(t / RISE_END)
            FlightPose(mix(from.center, s.center, e), mix(from.width, s.width, e), mix(from.height, s.height, e), mix(fromCorner, stage.corner, e), e.coerceIn(0f, 1f))
        }
        t < HOLD_END -> FlightPose(s.center, s.width, s.height, stage.corner, 1f)
        else -> {
            val e = TravelEasing.transform((t - HOLD_END) / (1f - HOLD_END))
            FlightPose(mix(s.center, end.center, e), mix(s.width, end.slot, e), mix(s.height, end.slot, e), mix(stage.corner, end.corner, e), 1f - e)
        }
    }
}
