package com.frameout.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.roundToInt

private const val PAD_DP = 12
private const val HAPTIC_STEPS = 20

@Composable
fun FrameSlider(
    value: Float,
    onChange: (Float) -> Unit,
    min: Float,
    max: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    val latestChange by rememberUpdatedState(onChange)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
    val colors = FrameTheme.colors
    val haptics = rememberFrameHaptics()
    val lastStep = remember { intArrayOf(-1) }
    val onFraction: (Float) -> Unit = { f ->
        val step = (f * HAPTIC_STEPS).roundToInt()
        if (step != lastStep[0]) {
            lastStep[0] = step
            haptics.perform(if (step == 0 || step == HAPTIC_STEPS) FrameHaptic.Select else FrameHaptic.Tick)
        }
        latestChange(min + f * (max - min))
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(min, max), min..max)
                setProgress { target -> latestChange(target.coerceIn(min, max)); true }
            }
            .sliderInput(listOf(min, max, rtl), rtl, onFraction)
    ) { drawSlider(fraction, rtl, colors.outline, colors.primary) }
}

private fun toFraction(x: Float, width: Int, pad: Float, rtl: Boolean): Float {
    val raw = ((x - pad) / (width - pad * 2f)).coerceIn(0f, 1f)
    return if (rtl) 1f - raw else raw
}

private fun Modifier.sliderInput(key: Any, rtl: Boolean, onFraction: (Float) -> Unit): Modifier = this
    .pointerInput(key) {
        val pad = PAD_DP.dp.toPx()
        detectTapGestures { onFraction(toFraction(it.x, size.width, pad, rtl)) }
    }
    .pointerInput(key) {
        val pad = PAD_DP.dp.toPx()
        detectHorizontalDragGestures(
            onDragStart = { onFraction(toFraction(it.x, size.width, pad, rtl)) },
            onHorizontalDrag = { change, _ ->
                change.consume()
                onFraction(toFraction(change.position.x, size.width, pad, rtl))
            }
        )
    }

private fun DrawScope.drawSlider(fraction: Float, rtl: Boolean, track: Color, fill: Color) {
    val pad = PAD_DP.dp.toPx()
    val usable = size.width - pad * 2f
    val start = pad + if (rtl) usable else 0f
    val thumb = pad + usable * (if (rtl) 1f - fraction else fraction)
    val cy = size.height / 2f
    val stroke = 4.dp.toPx()
    drawLine(track, Offset(pad, cy), Offset(size.width - pad, cy), stroke, StrokeCap.Round)
    drawLine(fill, Offset(start, cy), Offset(thumb, cy), stroke, StrokeCap.Round)
    drawCircle(fill, 10.dp.toPx(), Offset(thumb, cy))
}
