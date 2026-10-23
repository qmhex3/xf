package com.frameout.app.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.delay

private val StageHeight = 264.dp
private val StripHeight = 24.dp
private val SprocketPitch = 28.dp

@Composable
internal fun AboutHero(scroll: ScrollState) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val lock = remember { Animatable(if (reduce) 1f else 0f) }
    val textIn = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) lock.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay(320)
        textIn.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeroStage(scroll, lock, reduce)
        HeroTexts(textIn)
    }
}

@Composable
private fun HeroStage(scroll: ScrollState, lock: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>, reduce: Boolean) {
    val colors = FrameTheme.colors
    val strength = if (colors.isDark) 1f else 0.7f
    Box(
        Modifier
            .fillMaxWidth()
            .height(StageHeight)
            .graphicsLayer {
                val s = scroll.value.toFloat()
                translationY = s * 0.35f
                alpha = (1f - s / 420.dp.toPx()).coerceIn(0f, 1f)
            }
            .clip(FrameShapes.xxl)
            .drawBehind {
                val k = lock.value.coerceIn(0f, 1f) * strength
                drawRect(colors.container)
                drawRect(
                    Brush.radialGradient(
                        listOf(colors.accentA.copy(alpha = 0.55f * k), Color.Transparent),
                        center = Offset(size.width * 0.16f, size.height * 0.12f),
                        radius = size.width * 0.78f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(colors.accentB.copy(alpha = 0.46f * k), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.96f),
                        radius = size.width * 0.82f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(colors.accentC.copy(alpha = 0.26f * k), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.08f),
                        radius = size.width * 0.46f
                    )
                )
            }
    ) {
        FilmEdge(reduce, Modifier.align(Alignment.TopCenter))
        FilmEdge(reduce, Modifier.align(Alignment.BottomCenter))
        Box(Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
            Viewfinder(lock)
            BrandMark(size = 112.dp)
        }
    }
}

@Composable
private fun FilmEdge(reduce: Boolean, modifier: Modifier) {
    val loop = rememberInfiniteTransition(label = "aboutFilm")
    val phaseAnim by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "phase"
    )
    val phase = if (reduce) 0f else phaseAnim
    val ink = FrameTheme.colors.onSurface
    Canvas(modifier.fillMaxWidth().height(StripHeight)) {
        drawRect(Color.Black.copy(alpha = 0.16f))
        val hole = Size(12.dp.toPx(), 8.dp.toPx())
        val pitch = SprocketPitch.toPx()
        val y = (size.height - hole.height) / 2f
        var x = -pitch + phase * pitch
        while (x < size.width) {
            drawRoundRect(ink.copy(alpha = 0.22f), Offset(x, y), hole, CornerRadius(2.5.dp.toPx()))
            x += pitch
        }
    }
}

@Composable
private fun Viewfinder(lock: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>) {
    val ink = FrameTheme.colors.onSurface
    Canvas(Modifier.size(168.dp)) {
        val t = lock.value
        val spread = (1f - t).coerceAtLeast(0f) * 44.dp.toPx()
        val arm = 26.dp.toPx()
        val left = -spread
        val top = -spread
        val right = size.width + spread
        val bottom = size.height + spread
        val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val color = ink.copy(alpha = (0.25f + 0.65f * t).coerceIn(0f, 1f))
        fun corner(cx: Float, cy: Float, dx: Float, dy: Float) {
            val p = Path().apply {
                moveTo(cx + dx * arm, cy)
                lineTo(cx, cy)
                lineTo(cx, cy + dy * arm)
            }
            drawPath(p, color, style = stroke)
        }
        corner(left, top, 1f, 1f)
        corner(right, top, -1f, 1f)
        corner(left, bottom, 1f, -1f)
        corner(right, bottom, -1f, -1f)
    }
}

@Composable
private fun HeroTexts(enter: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>) {
    val colors = FrameTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(top = 20.dp).graphicsLayer {
            alpha = enter.value
            translationY = (1f - enter.value) * 14.dp.toPx()
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
