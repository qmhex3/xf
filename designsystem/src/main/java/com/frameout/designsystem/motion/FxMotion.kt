package com.frameout.designsystem.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val FxEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun <T> fxBouncy(): FiniteAnimationSpec<T> =
    if (LocalSystemA11y.current.reduceMotion) snap() else spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)

@Composable
fun fxBubbleEnter(): EnterTransition =
    if (LocalSystemA11y.current.reduceMotion) EnterTransition.None
    else fadeIn(tween(120)) + scaleIn(spring(0.55f, Spring.StiffnessMedium), 0.6f, TransformOrigin(1f, 0.5f))

@Composable
fun fxBubbleExit(): ExitTransition =
    if (LocalSystemA11y.current.reduceMotion) ExitTransition.None
    else fadeOut(tween(110)) + scaleOut(tween(140), 0.8f, TransformOrigin(1f, 0.5f))

@Composable
fun Modifier.fxAppear(key: Any, seen: MutableSet<Any>, order: Int): Modifier {
    val reduce = LocalSystemA11y.current.reduceMotion
    val first = remember(key) { !reduce && seen.add(key) }
    val progress = remember(key) { Animatable(if (first) 0f else 1f) }
    LaunchedEffect(key) {
        if (!first) return@LaunchedEffect
        delay(order.coerceIn(0, 10) * 28L)
        progress.animateTo(1f, tween(340, easing = FxEase))
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 16.dp.toPx()
        scaleX = 0.94f + 0.06f * p
        scaleY = 0.94f + 0.06f * p
    }
}

@Composable
fun Modifier.fxRise(delayMs: Int = 0): Modifier {
    val reduce = LocalSystemA11y.current.reduceMotion
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(420, easing = FxEase))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 18.dp.toPx()
    }
}

@Composable
fun Modifier.fxPop(): Modifier {
    val reduce = LocalSystemA11y.current.reduceMotion
    val scale = remember { Animatable(if (reduce) 1f else 0.3f) }
    LaunchedEffect(Unit) {
        if (!reduce) scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        alpha = scale.value.coerceIn(0f, 1f)
    }
}

@Composable
fun Modifier.fxFloat(amplitude: Dp = 5.dp, periodMs: Int = 2600): Modifier {
    if (LocalSystemA11y.current.reduceMotion) return this
    val transition = rememberInfiniteTransition(label = "fxFloat")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "fxFloatPhase")
    return graphicsLayer { translationY = sin(phase * 2f * PI.toFloat()) * amplitude.toPx() }
}

@Composable
fun Modifier.fxShake(trigger: Any?): Modifier {
    val reduce = LocalSystemA11y.current.reduceMotion
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (reduce) return@LaunchedEffect
        for (target in floatArrayOf(-1f, 1f, -0.6f, 0.6f, 0f)) offset.animateTo(target, tween(55, easing = LinearEasing))
    }
    return graphicsLayer { translationX = offset.value * 8.dp.toPx() }
}

@Composable
fun Modifier.fxPulse(trigger: Any?): Modifier {
    val reduce = LocalSystemA11y.current.reduceMotion
    val scale = remember { Animatable(1f) }
    val skipFirst = remember { booleanArrayOf(true) }
    LaunchedEffect(trigger) {
        if (skipFirst[0]) { skipFirst[0] = false; return@LaunchedEffect }
        if (reduce) return@LaunchedEffect
        scale.animateTo(1.28f, tween(90))
        scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

@Composable
fun Modifier.fxSheen(active: Boolean): Modifier {
    if (!active || LocalSystemA11y.current.reduceMotion) return this
    val transition = rememberInfiniteTransition(label = "fxSheen")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "fxSheenPhase")
    return drawWithContent {
        drawContent()
        val band = size.width * 0.35f
        val x = (size.width + band) * phase - band
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent), startX = x, endX = x + band))
    }
}
