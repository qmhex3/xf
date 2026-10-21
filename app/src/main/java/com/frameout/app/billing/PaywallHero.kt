package com.frameout.app.billing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.cos
import kotlin.math.sin

private const val PI_F = 3.1415927f
private const val INTRO_SECONDS = 1.5f

internal class HeroPalette(val a: Color, val b: Color, val c: Color, val glow: Float)

private class Spark(val x: Float, val y: Float, val phase: Float, val speed: Float)

private val Sparks = kotlin.random.Random(7).let { r ->
    List(26) { Spark(r.nextFloat(), r.nextFloat(), r.nextFloat() * 6.28f, 0.6f + r.nextFloat() * 1.4f) }
}

@Composable
private fun rememberHeroClock(reduce: Boolean): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(reduce) {
        if (reduce) {
            seconds.floatValue = INTRO_SECONDS
            return@LaunchedEffect
        }
        val start = androidx.compose.runtime.withFrameNanos { it }
        while (true) androidx.compose.runtime.withFrameNanos { seconds.floatValue = (it - start) / 1_000_000_000f }
    }
    return seconds
}

@Composable
fun PaywallHero(modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val clock = rememberHeroClock(reduce)
    val palette = remember(colors.accentA, colors.accentB, colors.accentC, colors.isDark) {
        HeroPalette(colors.accentA, colors.accentB, colors.accentC, if (colors.isDark) 1f else 0.55f)
    }
    Canvas(modifier.fillMaxWidth().height(172.dp).clearAndSetSemantics { }) { drawHero(palette, clock.value, reduce) }
}

private fun DrawScope.drawHero(p: HeroPalette, t: Float, calm: Boolean) {
    val intro = (t / INTRO_SECONDS).coerceIn(0f, 1f)
    val open = if (calm) 1f else 1f - (1f - intro) * (1f - intro) * (1f - intro)
    val motion = if (calm) 0f else t
    val pulse = if (calm) 0f else sin(t * 2f) * 0.5f + 0.5f
    val center = Offset(size.width / 2f, size.height * 0.5f)
    drawAurora(p, motion)
    drawSparks(p, t, calm)
    val cards = orbitCards(center, motion)
    cards.filter { it.z < 0f }.forEach { drawFrameCard(it, p, 0.55f * open) }
    drawRings(p, center, open, pulse)
    drawIris(p, center, open, motion, calm)
    drawCore(p, center, open, pulse, motion)
    cards.filter { it.z >= 0f }.forEach { drawFrameCard(it, p, (0.8f + 0.2f * it.z) * open) }
}

private fun DrawScope.drawAurora(p: HeroPalette, t: Float) {
    val spots = listOf(Triple(p.a, Offset(0.5f, 0.28f), 0f), Triple(p.c, Offset(0.78f, 0.45f), 2.1f), Triple(p.b, Offset(0.3f, 0.5f), 4.2f))
    spots.forEach { (color, at, phase) ->
        val x = size.width * at.x + sin(t * 0.5f + phase) * 34.dp.toPx()
        val y = size.height * at.y + cos(t * 0.4f + phase) * 18.dp.toPx()
        val stops = listOf(color.copy(alpha = 0.42f * p.glow), Color.Transparent)
        drawCircle(Brush.radialGradient(stops, Offset(x, y), size.width * 0.5f), size.width * 0.5f, Offset(x, y))
    }
}

private fun DrawScope.drawSparks(p: HeroPalette, t: Float, calm: Boolean) {
    Sparks.forEach { s ->
        val twinkle = if (calm) 0.6f else sin(t * s.speed * 1.6f + s.phase) * 0.5f + 0.5f
        val at = Offset(s.x * size.width, s.y * size.height * 0.92f)
        drawCircle(p.c.copy(alpha = 0.15f + twinkle * 0.7f), (0.7f + twinkle * 1.6f).dp.toPx(), at)
    }
}

private fun DrawScope.drawRings(p: HeroPalette, center: Offset, open: Float, pulse: Float) {
    val base = 54.dp.toPx()
    for (i in 1..2) {
        val radius = base + (10 + 13 * i + pulse * 5f * i).dp.toPx()
        drawCircle(p.a.copy(alpha = (0.34f - i * 0.12f) * open), radius, center, style = Stroke(1.2.dp.toPx()))
    }
}

private fun polar(c: Offset, r: Float, a: Float) = Offset(c.x + cos(a) * r, c.y + sin(a) * r)

private fun DrawScope.drawIris(p: HeroPalette, c: Offset, open: Float, t: Float, calm: Boolean) {
    val r = 54.dp.toPx()
    val third = PI_F / 3f
    val rot = t * 0.22f + (1f - open) * 1.6f
    val inner = r * (0.16f + 0.54f * open + if (calm) 0f else sin(t * 1.3f) * 0.035f)
    for (i in 0 until 6) {
        val a0 = rot + i * third
        val blade = Path().apply {
            val start = polar(c, r, a0)
            moveTo(start.x, start.y)
            listOf(polar(c, r, a0 + third * 1.55f), polar(c, inner, a0 + third * 1.1f + 1f), polar(c, inner, a0 + 1f)).forEach { lineTo(it.x, it.y) }
            close()
        }
        val fill = Brush.linearGradient(listOf((if (i % 2 == 1) p.b else p.a).copy(alpha = 0.95f), p.c.copy(alpha = 0.8f)), polar(c, r, a0), c)
        drawPath(blade, fill)
        drawPath(blade, Color.White.copy(alpha = 0.35f), style = Stroke(1.dp.toPx()))
    }
    drawCircle(Color.White.copy(alpha = 0.4f), r, c, style = Stroke(2.dp.toPx()))
}

private fun starPath(outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val radius = if (i % 2 == 1) inner else outer
        val angle = -PI_F / 2f + i * PI_F / 5f
        if (i == 0) moveTo(cos(angle) * radius, sin(angle) * radius) else lineTo(cos(angle) * radius, sin(angle) * radius)
    }
    close()
}

private fun DrawScope.drawCore(p: HeroPalette, c: Offset, open: Float, pulse: Float, t: Float) {
    val glowRadius = 54.dp.toPx() * 0.8f
    val stops = listOf(Color.White.copy(alpha = 0.95f * open), p.c.copy(alpha = 0.5f * open), p.c.copy(alpha = 0f))
    drawCircle(Brush.radialGradient(stops, c, glowRadius), glowRadius, c)
    val grow = (0.6f + 0.4f * open) * (1f + pulse * 0.04f)
    withTransform({
        translate(c.x, c.y)
        scale(grow, grow, Offset.Zero)
        rotate(sin(t) * 3.4f, Offset.Zero)
    }) { drawPath(starPath(17.dp.toPx(), 7.5.dp.toPx()), Color.White) }
}
