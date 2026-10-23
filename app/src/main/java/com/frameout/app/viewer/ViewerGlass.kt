package com.frameout.app.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.glass.shouldDisableGlass
import com.frameout.designsystem.theme.FrameColors
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import androidx.compose.ui.unit.dp

internal object ViewerInk {
    val text: Color
        @Composable get() = FrameTheme.colors.let { if (it.isDark) it.onSurface else FrameColors.DarkOnSurface }
    val accent: Color
        @Composable get() = FrameTheme.colors.onDarkAccent
    val danger: Color
        @Composable get() = FrameTheme.colors.let { if (it.isDark) it.error else FrameColors.DarkError }
}

@Composable
internal fun ViewerGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = FrameShapes.pill,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val solid = shouldDisableGlass(context, FrameTheme.glassIntensity, LocalSystemA11y.current.reduceTransparency)
    val base: Modifier = if (solid) Modifier.background(Color(0xFF161618)) else Modifier.background(Color.Black.copy(alpha = 0.45f))
    val edge = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.06f)))
    Box(
        modifier = modifier
            .clip(shape)
            .then(base)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.05f))))
            .border(1.dp, edge, shape),
        content = content
    )
}

@Composable
internal fun Modifier.viewerActiveGlow(color: Color = ViewerInk.accent): Modifier =
    this.frameGlow(color, { 0.32f }, 8.dp, { 0.5f })
