package com.frameout.app.extract

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameCheck
import com.frameout.designsystem.theme.FrameAccent
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.PI
import kotlin.math.sin

private const val CARDS = 3
private val RESTING = floatArrayOf(0.2f, 0.5f, 0.8f)

@Composable
internal fun ExtractFlow(running: Boolean, finished: Boolean) {
    if (finished) {
        FrameCheck(FrameTheme.colors.success, Modifier.size(36.dp), 22.dp)
        return
    }
    val moving = running && !LocalSystemA11y.current.reduceMotion
    val transition = rememberInfiniteTransition(label = "extractFlow")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "extractFlowPhase")
    val accent = FrameTheme.accent
    Canvas(Modifier.size(width = 36.dp, height = 24.dp)) { drawFlow(phase, moving, running, accent) }
}

private fun DrawScope.drawFlow(phase: Float, moving: Boolean, running: Boolean, accent: FrameAccent) {
    val cardW = size.width * 0.36f
    val cardH = size.height * 0.78f
    val radius = CornerRadius(cardH * 0.22f)
    for (k in 0 until CARDS) {
        val t = if (moving) (phase + k / CARDS.toFloat()) % 1f else RESTING[k]
        val alpha = (if (running) sin(PI.toFloat() * t) else 0.45f).coerceIn(0f, 1f)
        val left = (size.width + cardW) * t - cardW
        val top = (size.height - cardH) / 2f
        val fill = Brush.linearGradient(listOf(accent.b, accent.a, accent.c), Offset(left, top), Offset(left + cardW, top + cardH))
        drawRoundRect(fill, Offset(left, top), Size(cardW, cardH), radius, alpha = alpha)
    }
}
