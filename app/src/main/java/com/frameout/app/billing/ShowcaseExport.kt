package com.frameout.app.billing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private val Cards = listOf("GIF", "PDF", "ZIP", "CSV", "WebP")
private const val FAN_STEP_DEG = 21f
private const val FAN_STAGGER_MS = 90L

@Composable
internal fun ExportShowcase() {
    Box(Modifier.size(width = 200.dp, height = 140.dp), Alignment.BottomCenter) {
        Cards.forEachIndexed { index, name -> FanCard(name, index) }
    }
}

@Composable
private fun FanCard(name: String, index: Int) {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val open = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay(250L + FAN_STAGGER_MS * index)
        open.animateTo(1f, spring(0.55f, Spring.StiffnessLow))
    }
    val shape = RoundedCornerShape(12.dp)
    val brush = Brush.linearGradient(listOf(colors.accentC, colors.accentA, colors.accentB))
    Box(
        Modifier
            .padding(bottom = 4.dp)
            .size(width = 60.dp, height = 80.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1.4f)
                rotationZ = (index - (Cards.size - 1) / 2f) * FAN_STEP_DEG * open.value
                val s = 0.2f + 0.8f * open.value
                scaleX = s
                scaleY = s
                alpha = open.value.coerceIn(0f, 1f)
            }
            .background(brush, shape)
            .border(1.dp, Color.White.copy(alpha = 0.4f), shape),
        Alignment.TopCenter
    ) {
        FrameText(name, FrameTheme.typography.label.copy(fontFamily = FontFamily.Monospace), Color.White, Modifier.padding(top = 9.dp))
        FrameText(".${name.lowercase()}", FrameTheme.typography.bodySmall, Color.White.copy(alpha = 0.9f), Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp))
    }
}
