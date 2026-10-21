package com.frameout.app.billing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.frameout.core.pro.ProShowcase
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private class FormatBar(val label: String, val size: String, val share: Float)

private val Bars = listOf(FormatBar("JPG", "4.8 MB", 1f), FormatBar("HEIC", "2.1 MB", 0.44f), FormatBar("AVIF", "1.6 MB", 0.33f))
private val BarEase = CubicBezierEasing(0.2f, 1f, 0.3f, 1f)

@Composable
internal fun FormatShowcase() {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val badge = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay(1400)
        badge.animateTo(1f, spring(0.5f, Spring.StiffnessMediumLow))
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Bars.forEachIndexed { index, bar -> BarRow(bar, index, reduce) }
        FrameText(
            ProShowcase.saveSize(),
            FrameTheme.typography.label,
            proOnBrush(),
            Modifier
                .graphicsLayer { scaleX = badge.value; scaleY = badge.value; alpha = badge.value.coerceIn(0f, 1f) }
                .background(Brush.horizontalGradient(listOf(colors.accentB, colors.accentA)), FrameShapes.pill)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BarRow(bar: FormatBar, index: Int, reduce: Boolean) {
    val colors = FrameTheme.colors
    val fill = remember { Animatable(if (reduce) bar.share else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay(400L + 250L * index)
        fill.animateTo(bar.share, tween(1200, easing = BarEase))
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FrameText(bar.label, FrameTheme.typography.label.copy(fontFamily = FontFamily.Monospace), colors.onSurface, Modifier.width(44.dp))
        Box(Modifier.weight(1f).height(12.dp).clip(FrameShapes.pill).background(colors.onSurface.copy(alpha = 0.09f))) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .drawBehind {
                        val brush = Brush.horizontalGradient(listOf(colors.accentC, colors.accentA, colors.accentB))
                        drawRoundRect(brush, size = Size(size.width * fill.value, size.height), cornerRadius = CornerRadius(size.height / 2f))
                    }
            )
        }
        FrameText(bar.size, FrameTheme.typography.label.copy(fontFamily = FontFamily.Monospace), colors.onSurfaceVariant, Modifier.width(56.dp))
    }
}
