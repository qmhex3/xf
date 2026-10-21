package com.frameout.app.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.hypot
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.R
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme

private const val TOTAL_MS = 2250f
private const val EXIT_START_MS = 1500f
private const val MAX_STEP_MS = 40f
private const val TILE_FRACTION = 0.36f
private const val CENTER_FRACTION = 0.46f

@Composable
fun BrandSplash(onFinished: () -> Unit) {
    PreferredFrameTheme { BrandSplashContent(onFinished) }
}

@Composable
private fun BrandSplashContent(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    if (LocalSystemA11y.current.reduceMotion) {
        LaunchedEffect(Unit) { finish() }
        return
    }
    val clock = remember { mutableFloatStateOf(0f) }
    var skip by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runSplashClock(clock) { skip }
        finish()
    }
    BoxWithConstraints(
        Modifier.fillMaxSize().clearAndSetSemantics { }.pointerInput(Unit) { detectTapGestures { skip = true } }
    ) {
        val tileDp = minOf(maxWidth, maxHeight) * TILE_FRACTION
        val nameTop = maxHeight * CENTER_FRACTION + tileDp / 2 + 28.dp
        SplashBackdrop(clock)
        SplashContent(clock) { SplashName(clock, nameTop) }
    }
}

private suspend fun runSplashClock(clock: MutableFloatState, skipRequested: () -> Boolean) {
    var last = withFrameNanos { it }
    while (clock.floatValue < TOTAL_MS) {
        val now = withFrameNanos { it }
        val step = ((now - last) / 1_000_000f).coerceIn(0f, MAX_STEP_MS)
        last = now
        val next = clock.floatValue + step
        clock.floatValue = if (skipRequested() && next < EXIT_START_MS) EXIT_START_MS else next
    }
}

private fun exitProgress(t: Float): Float = inOut(seg(t, EXIT_START_MS, TOTAL_MS))

@Composable
private fun SplashBackdrop(clock: MutableFloatState) {
    val bg = FrameTheme.colors.background
    val ring = FrameTheme.accent.a
    Canvas(Modifier.fillMaxSize()) {
        val e = exitProgress(clock.floatValue)
        if (e <= 0f) {
            drawRect(bg)
            return@Canvas
        }
        val center = Offset(size.width / 2f, size.height * CENTER_FRACTION)
        val reach = hypot(maxOf(center.x, size.width - center.x), maxOf(center.y, size.height - center.y))
        val radius = reach * outCubic(e)
        val hole = Path().apply { addOval(Rect(center, radius)) }
        clipPath(hole, clipOp = ClipOp.Difference) { drawRect(bg) }
        val fade = (1f - e).coerceIn(0f, 1f)
        drawCircle(ring.copy(alpha = 0.8f * fade), radius, center, style = Stroke(width = 1f + 3.dp.toPx() * fade))
    }
}

@Composable
private fun SplashContent(clock: MutableFloatState, label: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    val accent = FrameTheme.accent
    val paths = remember { SplashPaths() }
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            val e = exitProgress(clock.floatValue)
            alpha = 1f - seg(e, 0f, 0.55f)
            val grow = 1f + 0.45f * outCubic(e)
            scaleX = grow
            scaleY = grow
            transformOrigin = TransformOrigin(0.5f, CENTER_FRACTION)
        }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val tile = minOf(size.width, size.height) * TILE_FRACTION
            drawBrandTile(clock.floatValue, Offset(size.width / 2f, size.height * CENTER_FRACTION), tile, accent, paths)
        }
        label()
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.SplashName(clock: MutableFloatState, top: androidx.compose.ui.unit.Dp) {
    FrameText(
        text = stringResource(R.string.app_name),
        style = FrameTheme.typography.title,
        color = FrameTheme.colors.onSurface,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = top)
            .graphicsLayer {
                val p = outCubic(seg(clock.floatValue, 1000f, 1400f))
                alpha = p
                translationY = (1f - p) * 10.dp.toPx()
            }
    )
}
