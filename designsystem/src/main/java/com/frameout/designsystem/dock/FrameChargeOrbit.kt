package com.frameout.designsystem.dock

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val ORBIT_PERIOD_MS = 3400

private val OrbitGap = 5.dp

private val MoonRadius = 4.dp

internal class OrbitState(
    val angle: () -> Float,
    val sweep: () -> Float,
    val visible: () -> Float,
    val running: Boolean,
    val track: Color,
    val arc: Color,
    val moonLight: Color,
    val moonBody: Color,
    val halo: Color
)

@Composable
internal fun rememberOrbit(running: Boolean, progress: Float?, visibleTarget: Float): OrbitState {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val spin: State<Float>? = if (running && !reduce) {
        rememberInfiniteTransition(label = "moonSpin").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(ORBIT_PERIOD_MS, easing = LinearEasing)),
            label = "moonAngle"
        )
    } else {
        null
    }
    val frac by animateFloatAsState((progress ?: 0f).coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing), label = "orbitFraction")
    val dark = colors.isDark
    return OrbitState(
        angle = { if (spin != null) spin.value - 90f else -90f + frac * 360f },
        sweep = { if (progress != null) frac * 360f else 0f },
        visible = { visibleTarget },
        running = running,
        track = if (dark) Color.White.copy(alpha = 0.14f) else colors.onSurface.copy(alpha = 0.16f),
        arc = colors.accentA,
        moonLight = lerp(colors.accentC, Color.White, if (dark) 0.65f else 0.4f),
        moonBody = colors.accentA,
        halo = colors.accentC
    )
}

internal fun DrawScope.drawOrbit(o: OrbitState) {
    val v = o.visible()
    if (v <= 0.01f) return
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f + OrbitGap.toPx()
    val ringStroke = Stroke(1.5.dp.toPx())
    drawCircle(o.track.copy(alpha = o.track.alpha * v), r, c, style = ringStroke)

    val sweep = o.sweep()
    if (sweep > 0.5f) {
        drawArc(
            color = o.arc.copy(alpha = 0.9f * v),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    val a = o.angle()
    if (o.running) {
        val steps = 10
        val span = 70f
        for (i in 0 until steps) {
            val t = i / steps.toFloat()
            drawArc(
                color = o.halo.copy(alpha = 0.55f * (1f - t) * v),
                startAngle = a - span * (t + 1f / steps),
                sweepAngle = span / steps + 1f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke((2.6f * (1f - t * 0.7f)).dp.toPx(), cap = StrokeCap.Butt)
            )
        }
    }
    val rad = a * PI.toFloat() / 180f
    val m = Offset(c.x + r * cos(rad), c.y + r * sin(rad))
    val mr = MoonRadius.toPx()
    drawCircle(
        Brush.radialGradient(listOf(o.halo.copy(alpha = 0.45f * v), Color.Transparent), m, mr * 3.2f),
        mr * 3.2f,
        m
    )
    drawCircle(
        Brush.radialGradient(
            listOf(o.moonLight.copy(alpha = v), o.moonBody.copy(alpha = v)),
            Offset(m.x - mr * 0.35f, m.y - mr * 0.35f),
            mr * 1.5f
        ),
        mr,
        m
    )
}
