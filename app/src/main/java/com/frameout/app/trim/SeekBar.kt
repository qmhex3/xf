package com.frameout.app.trim

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.FrameHaptics
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.player.PlayerController
import com.frameout.engine.timeline.formatTimestamp

@Composable
internal fun SeekBar(controller: PlayerController, durationMs: Long, modifier: Modifier = Modifier) {
    val position by controller.position.collectAsState()
    var dragging by remember { mutableStateOf<Float?>(null) }
    val haptics = rememberFrameHaptics()
    val dur = durationMs.coerceAtLeast(1L)
    val fraction = dragging ?: (position.toFloat() / dur).coerceIn(0f, 1f)
    val track = FrameTheme.colors.containerHigh
    val fill = FrameTheme.colors.primary
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Canvas(
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .semantics {
                    stateDescription = formatTimestamp(position)
                    progressBarRangeInfo = ProgressBarRangeInfo(position.toFloat().coerceIn(0f, dur.toFloat()), 0f..dur.toFloat())
                }
                .seekInput(dur, controller, haptics) { dragging = it }
        ) { drawSeek(fraction, track, fill) }
    }
}

private fun Modifier.seekInput(durationMs: Long, controller: PlayerController, haptics: FrameHaptics, onDrag: (Float?) -> Unit): Modifier =
    pointerInput(durationMs) {
        detectTapGestures { controller.seekTo(msFor(it.x, size.width, durationMs)) }
    }.pointerInput(durationMs) {
        detectHorizontalDragGestures(
            onDragStart = { controller.scrub(true); haptics.perform(FrameHaptic.DragStart) },
            onDragEnd = { controller.scrub(false); onDrag(null); haptics.perform(FrameHaptic.DragEnd) },
            onDragCancel = { controller.scrub(false); onDrag(null) }
        ) { change, _ ->
            change.consume()
            val fraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
            onDrag(fraction)
            controller.seekTo((fraction * durationMs).toLong())
        }
    }

private fun msFor(x: Float, width: Int, durationMs: Long): Long =
    ((x / width.coerceAtLeast(1)).coerceIn(0f, 1f) * durationMs).toLong()

private fun DrawScope.drawSeek(fraction: Float, track: Color, fill: Color) {
    val y = size.height / 2f
    val thickness = 6.dp.toPx()
    val radius = CornerRadius(thickness / 2f)
    drawRoundRect(track, Offset(0f, y - thickness / 2f), Size(size.width, thickness), radius)
    drawRoundRect(fill, Offset(0f, y - thickness / 2f), Size(size.width * fraction, thickness), radius)
    drawCircle(Color.Black.copy(alpha = 0.35f), 11.dp.toPx(), Offset(size.width * fraction, y))
    drawCircle(fill, 9.dp.toPx(), Offset(size.width * fraction, y))
}
