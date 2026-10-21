package com.frameout.designsystem.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.GlassIntensity

private const val SURFACE_ALPHA = 0.86f

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = FrameShapes.lg,
    intensity: GlassIntensity = FrameTheme.glassIntensity,
    glow: Color = Color.Unspecified,
    fillAlpha: Float = SURFACE_ALPHA,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val disabled = shouldDisableGlass(context, intensity, LocalSystemA11y.current.reduceTransparency)
    val solid = disabled || intensity == GlassIntensity.Off
    val colors = FrameTheme.colors
    val dark = colors.isDark
    val base = if (dark) colors.containerHigh else colors.surface
    val fill = if (solid) base else base.copy(alpha = fillAlpha)
    val rimColor = if (dark) Color.White else colors.onSurface
    val rimScale = if (dark) 1f else 0.7f
    val border = rimColor.copy(alpha = glassBorderAlpha(if (solid) GlassIntensity.Off else intensity) * rimScale)
    val glowing = glow != Color.Unspecified
    val edge: Brush = if (glowing) {
        Brush.verticalGradient(listOf(rimColor.copy(alpha = if (dark) 0.18f else 0.10f), glow.copy(alpha = if (dark) 0.24f else 0.38f)))
    } else {
        SolidColor(border)
    }
    Box(
        modifier = modifier
            .then(if (glowing) Modifier.frameGlow(glow, { if (dark) 0.18f else 0.10f }, 12.dp) else Modifier)
            .then(if (dark) Modifier else Modifier.shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.18f)))
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape),
        content = content
    )
}
