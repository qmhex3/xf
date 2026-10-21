package com.frameout.app.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import java.util.Locale

internal val HeroExtraHeight: Dp = 104.dp

private const val FRAMES = 13
private val FILM_FRACTIONS = List(FRAMES) { 0.02f + 0.96f * it / (FRAMES - 1) }
private val GAP = 6.dp

@Composable
internal fun StageCard(
    side: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.lg
    val rim = Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.40f), frameHairline(), colors.container))
    Box(
        modifier
            .size(side)
            .frameGlow(colors.primary, { 0.20f }, 22.dp, { (24f / side.value).coerceAtMost(0.5f) })
            .clip(shape)
            .background(Brush.verticalGradient(listOf(OnbColors.Ink, OnbColors.Night)))
            .border(1.dp, rim, shape),
        content = content
    )
}

@Composable
internal fun HeroStage(side: Dp, modifier: Modifier = Modifier) {
    val clip = rememberClip(OnboardingClips.HERO)
    val loop = rememberLoop(durationMs = 7000, still = 0.46f, reverse = true, label = "scrub", ease = true)
    val phase = clip?.phase ?: loop
    Column(
        modifier.width(side).clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StageCard(side) {
            if (clip != null) ClipSurface(clip.player, Modifier.fillMaxSize())
            else Canvas(Modifier.fillMaxSize()) { drawSplash(loop.value) }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Scrubber(phase, clip) }
    }
}

@Composable
private fun Scrubber(phase: State<Float>, clip: ClipHandle?) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.sm
    val frames = if (clip != null) rememberClipFrames(clip.path, FILM_FRACTIONS, 120, 100) else null
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(shape)
                .background(colors.containerHigh.copy(alpha = 0.7f))
                .border(1.dp, frameHairline(), shape)
                .padding(4.dp)
        ) {
            Filmstrip(maxWidth, phase, frames)
            Needle()
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioTicks()
            TimecodePill(phase, clip)
        }
    }
}

@Composable
private fun Filmstrip(viewport: Dp, phase: State<Float>, frames: List<ImageBitmap?>?) {
    val shade = FrameTheme.colors.containerHigh
    val cell = (viewport - GAP * 4) / 5
    Box(Modifier.fillMaxSize().clip(FrameShapes.xs)) {
        FilmstripRow(viewport, cell, phase, frames)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to shade.copy(alpha = 0.9f),
                    0.25f to Color.Transparent,
                    0.75f to Color.Transparent,
                    1f to shade.copy(alpha = 0.9f)
                )
            )
        )
        Box(
            Modifier
                .width(cell)
                .fillMaxHeight()
                .align(Alignment.Center)
                .border(1.dp, OnbColors.CyanSoft.copy(alpha = 0.75f), RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun FilmstripRow(viewport: Dp, cell: Dp, phase: State<Float>, frames: List<ImageBitmap?>?) {
    Row(
        Modifier
            .fillMaxHeight()
            .wrapContentWidth(Alignment.Start, unbounded = true)
            .graphicsLayer {
                val step = (cell + GAP).toPx()
                translationX = viewport.toPx() / 2f - (cell.toPx() / 2f + phase.value * (FRAMES - 1) * step)
            },
        horizontalArrangement = Arrangement.spacedBy(GAP)
    ) {
        repeat(FRAMES) { k ->
            val cellModifier = Modifier.width(cell).fillMaxHeight().clip(RoundedCornerShape(3.dp))
            val frame = frames?.getOrNull(k)
            when {
                frames == null -> Canvas(cellModifier) { drawSplash(k / (FRAMES - 1f)) }
                frame != null -> Image(frame, null, cellModifier, contentScale = ContentScale.Crop)
                else -> Box(cellModifier.background(OnbColors.Night))
            }
        }
    }
}

@Composable
private fun BoxScope.Needle() {
    val c = OnbColors.CyanSoft
    Box(Modifier.align(Alignment.Center).fillMaxHeight().width(2.dp).frameGlow(c, { 0.5f }, 6.dp).background(c))
    Box(Modifier.align(Alignment.TopCenter).size(10.dp, 4.dp).clip(FrameShapes.pill).background(c))
    Box(Modifier.align(Alignment.BottomCenter).size(10.dp, 4.dp).clip(FrameShapes.pill).background(c))
}

@Composable
private fun AudioTicks() {
    val c = FrameTheme.colors.onSurfaceVariant.copy(alpha = 0.4f)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(8, 12, 6, 8, 14, 8).forEach { h ->
            Box(Modifier.width(2.dp).height(h.dp).clip(FrameShapes.pill).background(c))
        }
    }
}

@Composable
private fun TimecodePill(phase: State<Float>, clip: ClipHandle?) {
    val colors = FrameTheme.colors
    val pulse by rememberLoop(durationMs = 1000, still = 1f, reverse = true, label = "rec", ease = true)
    val totalMs = clip?.player?.duration?.takeIf { it > 0L } ?: 8000L
    val ms = (phase.value * totalMs).toLong()
    val text = String.format(Locale.US, "%02d:%02d.%03d", ms / 60000, (ms / 1000) % 60, ms % 1000)
    Row(
        Modifier
            .clip(FrameShapes.pill)
            .background(colors.containerHigh.copy(alpha = 0.9f))
            .border(1.dp, frameHairline(), FrameShapes.pill)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .graphicsLayer { alpha = 0.4f + 0.6f * pulse }
                .clip(FrameShapes.pill)
                .background(colors.success)
        )
        FrameText(
            text,
            style = FrameTheme.typography.monoSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
            color = colors.success
        )
    }
}
