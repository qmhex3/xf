package com.frameout.designsystem.dock

import com.frameout.designsystem.theme.FrameGradients
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private const val INDETERMINATE_SWEEP = 90f
private const val SPIN_MS = 1100

@Composable
fun FrameProgressFab(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ring: Boolean = false,
    progress: Float? = null,
    stateText: String? = null,
    pop: Boolean = false,
    containerColor: Color = FrameTheme.colors.primary,
    contentColor: Color = FrameTheme.colors.onPrimary
) {
    val haptics = rememberFrameHaptics()
    val container by animateColorAsState(containerColor, frameSpring(), label = "fabContainer")
    val scale by animateFloatAsState(if (pop) 1.1f else 1f, frameSpring(), label = "fabPop")
    val gradient = containerColor == FrameTheme.colors.primary
    val fill: Brush = if (gradient) FrameGradients.Fab else SolidColor(container)
    val tint = if (gradient && contentColor == FrameTheme.colors.onPrimary) Color.White else contentColor
    Box(
        modifier = modifier
            .size(56.dp)
            .scale(scale)
            .background(fill, FrameShapes.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.perform(FrameHaptic.Tap)
                    onClick()
                }
            )
            .semantics { stateText?.let { stateDescription = it } },
        contentAlignment = Alignment.Center
    ) {
        if (ring) ProgressRing(progress, tint)
        Crossfade(icon, animationSpec = frameSpring(), label = "fabIcon") { current ->
            Image(painterResource(current), contentDescription, Modifier.size(24.dp), colorFilter = ColorFilter.tint(tint))
        }
    }
}

@Composable
private fun ProgressRing(progress: Float?, color: Color) {
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    if (progress == null && !reduceMotion) {
        val angle = rememberInfiniteTransition(label = "ringSpin").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(SPIN_MS, easing = LinearEasing)),
            label = "ringAngle"
        )
        RingArcs(color, { angle.value - 90f }, { INDETERMINATE_SWEEP })
    } else {
        val sweep = animateFloatAsState((progress ?: 0.25f) * 360f, frameSpring(), label = "ringSweep")
        RingArcs(color, { -90f }, { sweep.value })
    }
}

@Composable
private fun RingArcs(color: Color, start: () -> Float, sweep: () -> Float) {
    Canvas(Modifier.fillMaxSize().padding(4.dp)) {
        val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
        drawArc(color.copy(alpha = 0.25f), 0f, 360f, false, style = stroke)
        drawArc(color, start(), sweep(), false, style = stroke)
    }
}
