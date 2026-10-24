package com.frameout.designsystem.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import com.frameout.designsystem.theme.FrameTheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.frameGlow(
    color: Color,
    alpha: () -> Float,
    spread: Dp = 16.dp,
    cornerFraction: () -> Float = { 0.5f }
): Modifier = composed {
    if (FrameTheme.colors.neutral) return@composed Modifier
    Modifier.drawBehind {
        val a = alpha()
        if (a <= 0.005f) return@drawBehind
        val spreadPx = spread.toPx()
        val base = size.minDimension * cornerFraction().coerceIn(0f, 0.5f)
        val steps = 8
        for (i in steps downTo 1) {
            val t = i / steps.toFloat()
            val grow = spreadPx * t
            val layer = (a * (1f - t) * 0.20f + a * 0.01f).coerceIn(0f, 1f)
            drawRoundRect(
                color = color.copy(alpha = layer),
                topLeft = Offset(-grow, -grow),
                size = Size(size.width + grow * 2f, size.height + grow * 2f),
                cornerRadius = CornerRadius(base + grow)
            )
        }
    }
}
