package com.frameout.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import kotlin.math.hypot
import kotlin.math.pow

@Stable
class ThemeWaveState {
    var center by mutableStateOf(Offset.Zero)
    var color by mutableStateOf(Color.Transparent)
    var tick by mutableIntStateOf(0)

    fun fire(at: Offset, tint: Color) {
        center = at
        color = tint
        tick += 1
    }
}

val LocalThemeWave = staticCompositionLocalOf { ThemeWaveState() }

@Stable
class ThemeWaveSource(private val wave: ThemeWaveState) {
    private var origin = Offset.Zero

    val modifier: Modifier
        get() = Modifier.onGloballyPositioned { c -> origin = c.positionInRoot() + Offset(c.size.width / 2f, c.size.height / 2f) }

    fun fire(color: Color) = wave.fire(origin, color)
}

@Composable
fun rememberThemeWaveSource(): ThemeWaveSource {
    val wave = LocalThemeWave.current
    return remember(wave) { ThemeWaveSource(wave) }
}

@Composable
fun ThemeWaveHost(state: ThemeWaveState) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val progress = remember { Animatable(1f) }
    LaunchedEffect(state.tick) {
        if (state.tick == 0 || reduce) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(720, easing = LinearEasing))
    }
    Canvas(Modifier.fillMaxSize()) { if (progress.value < 1f) drawWave(state, progress.value) }
}

private fun DrawScope.drawWave(state: ThemeWaveState, p: Float) {
    val reach = hypot(size.width, size.height) * (1f - (1f - p).pow(3))
    val fade = (1f - p).pow(1.5f)
    drawCircle(state.color.copy(alpha = 0.28f * fade), radius = reach, center = state.center)
    drawCircle(state.color.copy(alpha = 0.8f * fade), radius = reach, center = state.center, style = Stroke(width = 3.dp.toPx()))
}
