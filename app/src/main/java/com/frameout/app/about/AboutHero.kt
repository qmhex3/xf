package com.frameout.app.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.app.BuildConfig
import com.frameout.app.splash.BrandMark
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ModeStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun AboutHero() {
    val reduce = LocalSystemA11y.current.reduceMotion
    val enter = rememberEntrance(reduce) { spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow) }
    val textEnter = rememberEntrance(reduce) { tween(650, delayMillis = 380, easing = FastOutSlowInEasing) }
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(224.dp).graphicsLayer {
                val s = 0.82f + 0.18f * enter.value
                scaleX = s
                scaleY = s
                alpha = enter.value.coerceIn(0f, 1f)
            },
            contentAlignment = Alignment.Center
        ) {
            HeroGlow(reduce)
            BrandMark(size = 120.dp)
        }
        HeroTexts(textEnter)
    }
}

@Composable
private fun rememberEntrance(reduce: Boolean, spec: () -> AnimationSpec<Float>): Animatable<Float, AnimationVector1D> {
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) progress.animateTo(1f, spec())
    }
    return progress
}

@Composable
private fun HeroGlow(reduce: Boolean) {
    val colors = FrameTheme.colors
    val loop = rememberInfiniteTransition(label = "aboutHero")
    val pulseAnim by loop.animateFloat(
        initialValue = 0.86f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val spinAnim by loop.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "spin"
    )
    val pulse = if (reduce) 1f else pulseAnim
    val spin = if (reduce) 40f else spinAnim
    val a = colors.accentA
    val b = colors.accentB
    val strength = if (colors.isDark) 1f else 0.6f
    val ringTint = colors.onSurface.copy(alpha = if (colors.isDark) 0.07f else 0.09f)
    Canvas(Modifier.fillMaxSize()) {
        val c = center
        val r = size.minDimension / 2f
        val glow = Brush.radialGradient(
            listOf(a.copy(alpha = 0.42f * strength), b.copy(alpha = 0.16f * strength), Color.Transparent),
            center = c,
            radius = r * pulse
        )
        drawCircle(brush = glow, radius = r * pulse, center = c)
        drawCircle(color = ringTint, radius = r * 0.74f, center = c, style = Stroke(width = 1.dp.toPx()))
        val comet = Brush.sweepGradient(
            listOf(Color.Transparent, Color.Transparent, a.copy(alpha = 0.95f), b, Color.Transparent),
            center = c
        )
        rotate(spin, c) {
            drawCircle(brush = comet, radius = r * 0.74f, center = c, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun HeroTexts(enter: Animatable<Float, AnimationVector1D>) {
    val colors = FrameTheme.colors
    Column(
        Modifier.fillMaxWidth().graphicsLayer {
            alpha = enter.value
            translationY = (1f - enter.value) * 18.dp.toPx()
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FrameText(FrameStrings.appName(), style = FrameTheme.typography.display, textAlign = TextAlign.Center)
        FrameText(
            ModeStrings.aboutTagline(),
            style = FrameTheme.typography.body,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp)
        )
        GlassSurface(shape = FrameShapes.pill, modifier = Modifier.padding(top = 14.dp)) {
            FrameText(
                ModeStrings.aboutVersion(BuildConfig.VERSION_NAME),
                style = FrameTheme.typography.monoSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}
