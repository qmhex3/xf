package com.frameout.app.billing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameTheme

private const val CYCLE_MS = 7000
private const val FADE_FROM = 0.90f

@Composable
internal fun ShapesShowcase() {
    val colors = FrameTheme.colors
    val scene = rememberShowcaseScene()
    val reveal by animateFloatAsState(if (scene != null) 1f else 0f, tween(300), label = "sceneReveal")
    val cycle = rememberCycle(LocalSystemA11y.current.reduceMotion, CYCLE_MS, 0.82f)
    val box = Modifier.size(SCENE_W_DP.dp, SCENE_H_DP.dp).clip(RoundedCornerShape(16.dp)).graphicsLayer { alpha = reveal }
    Canvas(box) {
        val s = scene ?: return@Canvas
        val p = cycle.value
        val fade = if (p > FADE_FROM) ((1f - p) / (1f - FADE_FROM)).coerceIn(0f, 1f) else 1f
        drawImage(s.sharp)
        buildMarks(size, colors.accentC, colors.accentB, colors.accentA).forEach { drawMark(it, p, fade) }
        drawBadge(colors.accentA, p, fade)
        drawHandles(colors.accentB, p, fade)
    }
}
