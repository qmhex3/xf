package com.frameout.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import kotlin.math.hypot

object FlowEase {
    val Quint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val InOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}

val LocalPageActive = compositionLocalOf { true }

fun Modifier.staggerIn(
    index: Int,
    active: Boolean = true,
    distance: Dp = 22.dp,
    stepMs: Int = 70,
    baseMs: Int = 90
): Modifier = composed {
    val reduce = LocalSystemA11y.current.reduceMotion
    val spec: AnimationSpec<Float> = when {
        reduce -> snap()
        active -> tween(durationMillis = 560, delayMillis = baseMs + index * stepMs, easing = FlowEase.Quint)
        else -> tween(durationMillis = 140)
    }
    val p by animateFloatAsState(if (active) 1f else 0f, spec, label = "stagger")
    Modifier.graphicsLayer {
        alpha = p
        translationY = (1f - p) * distance.toPx()
        val s = 0.97f + 0.03f * p
        scaleX = s
        scaleY = s
    }
}

@Composable
fun IrisReveal(
    playing: Boolean,
    color: Color,
    ring: Color,
    origin: Offset = Offset(0.5f, 0.88f),
    durationMs: Int = 780,
    onFinished: () -> Unit = {}
) {
    if (!playing) return
    val reduce = LocalSystemA11y.current.reduceMotion
    val done by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!reduce) progress.animateTo(1f, tween(durationMs, easing = FlowEase.Emphasized))
        done()
    }
    if (reduce) return
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width * origin.x, size.height * origin.y)
        val reach = maxOf(
            hypot(center.x, center.y),
            hypot(size.width - center.x, center.y),
            hypot(center.x, size.height - center.y),
            hypot(size.width - center.x, size.height - center.y)
        )
        val p = progress.value
        val radius = reach * p
        val hole = Path().apply { addOval(Rect(center, radius)) }
        clipPath(hole, clipOp = ClipOp.Difference) { drawRect(color) }
        if (radius > 1f) {
            val fade = (1f - p).coerceIn(0f, 1f)
            drawCircle(ring.copy(alpha = 0.85f * fade), radius, center, style = Stroke(width = (1f + 3.dp.toPx() * fade)))
            drawCircle(ring.copy(alpha = 0.25f * fade), radius * 0.97f, center, style = Stroke(width = 10.dp.toPx() * fade))
        }
    }
}
