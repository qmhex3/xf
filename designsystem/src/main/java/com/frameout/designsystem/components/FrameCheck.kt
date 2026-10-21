package com.frameout.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import kotlin.math.min

@Composable
fun FrameCheck(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp, strokeWidth: Dp = 2.2.dp) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) progress.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    Canvas(modifier.size(size)) {
        drawRing(progress.value, color)
        drawCheck(progress.value, color, strokeWidth.toPx())
    }
}

private fun DrawScope.drawRing(progress: Float, color: Color) {
    if (progress >= 1f) return
    drawCircle(
        color = color.copy(alpha = 0.35f * (1f - progress)),
        radius = size.minDimension * (0.45f + 0.55f * progress),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

private fun DrawScope.drawCheck(progress: Float, color: Color, width: Float) {
    val a = Offset(size.width * 0.2f, size.height * 0.54f)
    val b = Offset(size.width * 0.42f, size.height * 0.74f)
    val c = Offset(size.width * 0.82f, size.height * 0.30f)
    val first = (b - a).getDistance()
    val second = (c - b).getDistance()
    val drawn = (first + second) * progress
    val firstShare = min(drawn, first) / first
    drawLine(color, a, a + (b - a) * firstShare, width, StrokeCap.Round)
    if (drawn > first) drawLine(color, b, b + (c - b) * ((drawn - first) / second), width, StrokeCap.Round)
}
