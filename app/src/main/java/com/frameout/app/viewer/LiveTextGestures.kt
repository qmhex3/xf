package com.frameout.app.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.Transform
import com.frameout.core.quality.ScannedWord

private val TOUCH_PAD = 22.dp
private const val DOUBLE_TAP_SLOP = 4f

private enum class Press { Tap, Cancelled, Drag }

private class LiveHit(
    val words: List<ScannedWord>,
    val start: Int,
    val width: Float,
    val height: Float,
    val imageWidth: Int,
    val imageHeight: Int
)

internal suspend fun PointerInputScope.detectLiveTextSelection(
    words: () -> List<ScannedWord>,
    transform: () -> Transform,
    imageWidth: Int,
    imageHeight: Int,
    holdToSelect: () -> Boolean,
    onSelect: (anchor: Int, focus: Int) -> Unit,
    onTapWord: (Int) -> Unit,
    onDoubleTap: (Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val list = words()
        val t0 = transform()
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        val start = if (list.isEmpty()) -1 else LiveTextGeometry.nearest(list, down.position, w, h, imageWidth, imageHeight, t0.scale, t0.x, t0.y, TOUCH_PAD.toPx())
        if (start < 0) return@awaitEachGesture
        down.consume()
        val hold = holdToSelect()
        when (awaitPress(down, hold)) {
            Press.Cancelled -> Unit
            Press.Tap -> {
                onTapWord(start)
                awaitSecondTap(down.position)?.let(onDoubleTap)
            }
            Press.Drag -> {
                if (hold) onSelect(start, start)
                dragSelect(down, LiveHit(list, start, w, h, imageWidth, imageHeight), transform, onSelect)
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitPress(down: PointerInputChange, hold: Boolean): Press {
    var travelled = Offset.Zero
    val loop: suspend AwaitPointerEventScope.() -> Press = {
        var result: Press? = null
        while (result == null) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            result = when {
                change == null || event.changes.count { it.pressed } >= 2 -> Press.Cancelled
                !change.pressed -> {
                    change.consume()
                    Press.Tap
                }
                else -> {
                    travelled += change.positionChange()
                    when {
                        travelled.getDistance() <= viewConfiguration.touchSlop -> null
                        hold -> Press.Cancelled
                        else -> {
                            change.consume()
                            Press.Drag
                        }
                    }
                }
            }
        }
        result
    }
    if (!hold) return loop()
    return withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { loop() } ?: Press.Drag
}

private suspend fun AwaitPointerEventScope.awaitSecondTap(first: Offset): Offset? {
    val second = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    } ?: return null
    if ((second.position - first).getDistance() > viewConfiguration.touchSlop * DOUBLE_TAP_SLOP) return null
    second.consume()
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == second.id } ?: return second.position
        change.consume()
        if (!change.pressed) return second.position
    }
}

private suspend fun AwaitPointerEventScope.dragSelect(
    down: PointerInputChange,
    hit: LiveHit,
    transform: () -> Transform,
    onSelect: (Int, Int) -> Unit
) {
    var focus = hit.start
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        if (event.changes.count { it.pressed } >= 2) return
        val change = event.changes.firstOrNull { it.id == down.id } ?: return
        if (!change.pressed) {
            change.consume()
            return
        }
        val t = transform()
        val idx = LiveTextGeometry.nearest(hit.words, change.position, hit.width, hit.height, hit.imageWidth, hit.imageHeight, t.scale, t.x, t.y)
        if (idx >= 0 && idx != focus) {
            focus = idx
            onSelect(hit.start, focus)
        } else if (idx == hit.start && focus == hit.start) {
            onSelect(hit.start, hit.start)
        }
        change.consume()
    }
}
