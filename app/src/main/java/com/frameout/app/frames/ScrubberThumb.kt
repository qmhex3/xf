package com.frameout.app.frames

import com.frameout.designsystem.theme.frameControl
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import com.frameout.designsystem.components.FrameRollingText
import com.frameout.designsystem.motion.fxBouncy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import androidx.compose.ui.res.painterResource
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.timeline.formatTimestamp

@Composable
fun ScrubberThumb(active: Boolean) {
    val colors = FrameTheme.colors
    val grow by animateFloatAsState(if (active) 1.14f else 1f, fxBouncy(), label = "thumbGrow")
    val glow by animateFloatAsState(if (active) 1f else 0.45f, tween(160), label = "thumbGlow")
    val chevron by animateColorAsState(if (active) Color.White else colors.primary, tween(160), label = "thumbChevron")
    val tint = ColorFilter.tint(chevron)
    Box(Modifier.size(width = 48.dp, height = 56.dp).graphicsLayer { scaleX = grow; scaleY = grow }, Alignment.Center) {
        Column(
            Modifier.size(width = 32.dp, height = 48.dp).frameControl(FrameShapes.pill, selected = true, solid = active, glowAlpha = glow, glowSpread = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painterResource(FrameIcons.Back), null, Modifier.size(16.dp).rotate(90f), colorFilter = tint)
            Image(painterResource(FrameIcons.Back), null, Modifier.size(16.dp).rotate(-90f), colorFilter = tint)
        }
    }
}

@Composable
fun ScrubberBubble(index: Int, total: Int, timeMs: Long?) {
    val colors = FrameTheme.colors
    Column(
        Modifier.padding(end = 8.dp).frameControl(FrameShapes.md, selected = true, glowAlpha = 0.7f, cornerFraction = 0.3f).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FrameText(timeMs?.let(::formatTimestamp) ?: "--:--.---", FrameTheme.typography.mono, colors.onSurface)
        FrameRollingText("${index + 1}/$total", FrameTheme.typography.monoSmall, colors.primary)
    }
}
