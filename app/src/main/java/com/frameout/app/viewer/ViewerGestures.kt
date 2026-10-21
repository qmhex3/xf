package com.frameout.app.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs

internal suspend fun PointerInputScope.detectViewportGestures(
    singleFinger: () -> Boolean,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var active = false
        var travelled = Offset.Zero
        do {
            val event = awaitPointerEvent()
            if (event.changes.any { it.isConsumed }) return@awaitEachGesture
            val pressed = event.changes.count { it.pressed }
            val multi = pressed >= 2
            if (multi || (singleFinger() && pressed == 1)) {
                val pan = event.calculatePan()
                travelled += pan
                if (multi || active || travelled.getDistance() > viewConfiguration.touchSlop) {
                    active = true
                    val zoom = event.calculateZoom()
                    if (zoom != 1f || pan != Offset.Zero) onGesture(event.calculateCentroid(useCurrent = true), pan, zoom)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

internal suspend fun PointerInputScope.detectVerticalSwipe(enabled: () -> Boolean, onSwipeDown: () -> Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        if (!enabled()) return@awaitEachGesture
        var total = Offset.Zero
        var resolved = false
        while (!resolved) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            if (change.isConsumed || event.changes.count { it.pressed } > 1) break
            total += change.position - change.previousPosition
            if (!change.pressed) break
            if (total.getDistance() > viewConfiguration.touchSlop) {
                val vertical = abs(total.y) > abs(total.x) * 1.35f
                if (vertical && total.y > 0f) {
                    change.consume()
                    onSwipeDown()
                    resolved = true
                } else if (!vertical) {
                    resolved = true
                }
            }
        }
    }
}
