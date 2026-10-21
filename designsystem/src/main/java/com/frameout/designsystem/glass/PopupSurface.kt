package com.frameout.designsystem.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardEdge

@Composable
fun Modifier.framePopupSurface(shape: Shape = FrameShapes.xl): Modifier {
    val colors = FrameTheme.colors
    val dark = colors.isDark
    val base = if (dark) colors.containerHigh else colors.surface
    val top = if (dark) lerp(base, Color.White, 0.06f) else base
    val glow = colors.primary.copy(alpha = if (dark) 0.14f else 0.08f)
    return this
        .shadow(22.dp, shape, ambientColor = Color.Black.copy(alpha = if (dark) 0.55f else 0.16f), spotColor = Color.Black.copy(alpha = if (dark) 0.6f else 0.24f))
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, base)))
        .drawBehind { drawRect(Brush.radialGradient(listOf(glow, Color.Transparent), Offset(size.width, 0f), size.width * 0.9f)) }
        .border(1.dp, frameCardEdge(), shape)
}

@Composable
fun Modifier.frameSheetSurface(shape: Shape): Modifier {
    val colors = FrameTheme.colors
    val dark = colors.isDark
    val base = if (dark) colors.container else colors.surface
    val top = if (dark) lerp(base, Color.White, 0.06f) else base
    val glow = colors.primary.copy(alpha = if (dark) 0.16f else 0.09f)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, base)))
        .drawBehind { drawRect(Brush.radialGradient(listOf(glow, Color.Transparent), Offset(size.width / 2f, 0f), size.width * 0.75f)) }
        .border(1.dp, frameCardEdge(colors.primary, if (dark) 0.40f else 0.28f), shape)
}
