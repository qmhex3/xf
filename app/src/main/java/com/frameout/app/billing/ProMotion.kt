package com.frameout.app.billing

import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameTheme

private const val DARK_INK = 0xFF0E1110

@Composable
fun rememberShimmer(periodMs: Int = 3400, delayMs: Int = 0): State<Float> {
    val reduce = LocalSystemA11y.current.reduceMotion
    if (reduce) return remember { mutableStateOf(-1f) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val spec = infiniteRepeatable<Float>(
        animation = keyframes {
            durationMillis = periodMs
            0f at 0
            0f at (periodMs * 0.55f).toInt()
            1f at periodMs
        },
        initialStartOffset = StartOffset(delayMs)
    )
    return transition.animateFloat(0f, 1f, spec, label = "shimmerValue")
}

fun Modifier.shimmerSweep(progress: State<Float>, strength: Float = 0.6f): Modifier = drawWithContent {
    drawContent()
    val p = progress.value
    if (p >= 0f) {
        val w = size.width
        val x = -w + 3f * w * p
        val sweep = listOf(Color.Transparent, Color.White.copy(alpha = strength), Color.Transparent)
        drawRect(Brush.linearGradient(sweep, Offset(x, 0f), Offset(x + w, size.height * 0.4f)))
    }
}

fun readableOn(background: Color): Color =
    if (background.luminance() > 0.42f) Color(DARK_INK) else Color.White

@Composable
fun proOnBrush(): Color {
    val c = FrameTheme.colors
    return readableOn(lerp(c.accentA, c.accentB, 0.5f))
}

@Composable
fun proBrush(): Brush = FrameGradients.Fab

fun cycleColors(palette: List<Color>, phase: Float, steps: Int = 12): List<Color> =
    List(steps + 1) { i ->
        val pos = ((i.toFloat() / steps + phase) % 1f) * palette.size
        val lo = pos.toInt() % palette.size
        lerp(palette[lo], palette[(lo + 1) % palette.size], pos - pos.toInt())
    }
