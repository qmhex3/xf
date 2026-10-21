package com.frameout.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.glass.frameGlow

@Composable
fun Modifier.frameControl(
    shape: Shape,
    selected: Boolean = false,
    tint: Color = FrameTheme.colors.primary,
    glowAlpha: Float = if (selected) 1f else 0f,
    glowSpread: Dp = 10.dp,
    cornerFraction: Float = 0.5f,
    solid: Boolean = false
): Modifier {
    val c = FrameTheme.colors
    val dark = c.isDark
    val fill: Brush = when {
        solid -> Brush.linearGradient(listOf(lerp(tint, Color.White, 0.18f), tint, lerp(tint, c.accentB, 0.35f)))
        selected -> Brush.verticalGradient(listOf(tint.copy(alpha = if (dark) 0.30f else 0.20f), tint.copy(alpha = if (dark) 0.14f else 0.08f)))
        else -> frameCardBrush()
    }
    val edge: Brush = when {
        solid -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.10f)))
        selected -> Brush.verticalGradient(listOf(tint.copy(alpha = 0.70f), tint.copy(alpha = 0.26f)))
        else -> frameCardEdge()
    }
    val glowBase = if (dark) 0.26f else 0.16f
    return this
        .then(if (glowAlpha > 0f) Modifier.frameGlow(tint, { glowBase * glowAlpha }, glowSpread, { cornerFraction }) else Modifier)
        .background(fill, shape)
        .border(1.dp, edge, shape)
}
