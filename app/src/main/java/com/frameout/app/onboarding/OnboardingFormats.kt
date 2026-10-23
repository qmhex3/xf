package com.frameout.app.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.motion.frameFadeIn
import com.frameout.designsystem.motion.frameFadeOut
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

internal const val FormatsHeightFactor = 0.703f
internal val FormatsExtraHeight = 48.dp

private val SlotGap = 6.dp
private val FormatNames = listOf("MP4", "MKV", "AVI", "GIF", "Live Photo")
internal val OnboardingModeKinds = listOf("every_frame", "every_n", "every_time", "keyframes", "fixed", "scene", "slides", "content")

private val CaptureAt = listOf(0.30f, 0.46f, 0.62f, 0.78f)
private const val BaseCount = 408
private const val TotalCount = 980

@Composable
internal fun FormatsStage(width: Dp, modifier: Modifier = Modifier) {
    val clip = rememberClip(OnboardingClips.FORMATS)
    val loop = rememberLoop(durationMs = 6000, still = 0.9f, reverse = false, label = "formats")
    val phase = clip?.phase ?: loop
    val frames = if (clip != null) rememberClipFrames(clip.path, CaptureAt, 240, 135) else null
    val filled by remember { derivedStateOf { CaptureAt.count { phase.value >= it } } }
    Column(modifier.width(width), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ClipCard(clip, phase, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SlotGap)) {
                    repeat(CaptureAt.size) { k ->
                        TraySlot(k, filled, frames?.getOrNull(k), frames != null, Modifier.weight(1f))
                    }
                }
            }
        }
        CounterRow(BaseCount + filled)
    }
}

@Composable
private fun ClipCard(clip: ClipHandle?, phase: State<Float>, modifier: Modifier) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.lg
    val rim = Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.40f), frameHairline(), colors.container))
    Box(
        modifier
            .frameGlow(colors.primary, { 0.20f }, 22.dp, { 0.08f })
            .clip(shape)
            .background(Brush.verticalGradient(listOf(OnbColors.Ink, OnbColors.Night)))
            .border(1.dp, rim, shape)
    ) {
        if (clip != null) ClipSurface(clip.player, Modifier.fillMaxSize())
        else Canvas(Modifier.fillMaxSize()) { drawSplash(phase.value) }
        Canvas(Modifier.fillMaxSize()) { drawCaptureEffects(phase.value) }
        CardHud(phase, clip)
    }
}

private fun DrawScope.drawCaptureEffects(p: Float) {
    var flash = 0f
    for (at in CaptureAt) flash = max(flash, (1f - abs(p - at) / 0.02f).coerceIn(0f, 1f))
    if (flash > 0f) drawRect(Color.White.copy(alpha = 0.35f * flash))
    val inset = 12.dp.toPx()
    val len = 18.dp.toPx()
    val stroke = 1.5.dp.toPx()
    val c = OnbColors.CyanSoft.copy(alpha = 0.35f + 0.5f * flash)
    val l = inset
    val t = inset
    val r = size.width - inset
    val b = size.height - inset
    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        drawLine(c, Offset(x, y), Offset(x + dx * len, y), stroke, StrokeCap.Square)
        drawLine(c, Offset(x, y), Offset(x, y + dy * len), stroke, StrokeCap.Square)
    }
    corner(l, t, 1f, 1f)
    corner(r, t, -1f, 1f)
    corner(l, b, 1f, -1f)
    corner(r, b, -1f, -1f)
}

@Composable
private fun CardHud(phase: State<Float>, clip: ClipHandle?) {
    val format by remember { derivedStateOf { (phase.value * FormatNames.size).toInt().coerceIn(0, FormatNames.size - 1) } }
    val tick by remember { derivedStateOf { (phase.value * 50f).toInt() } }
    val totalMs = clip?.player?.duration?.takeIf { it > 0L } ?: 6000L
    val ms = (tick / 50f * totalMs).toLong()
    val time = String.format(Locale.US, "%02d:%02d.%d", ms / 60000, (ms / 1000) % 60, (ms % 1000) / 100)
    val fadeIn = frameFadeIn()
    val fadeOut = frameFadeOut()
    val mono = FrameTheme.typography.monoSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    Box(Modifier.fillMaxSize().padding(10.dp)) {
        HudPill(Modifier.align(Alignment.TopEnd)) {
            Box(Modifier.size(5.dp).clip(FrameShapes.pill).background(OnbColors.CyanSoft))
            AnimatedContent(targetState = format, transitionSpec = { fadeIn togetherWith fadeOut }, label = "format") { i ->
                FrameText(FormatNames[i], style = mono, color = Color.White.copy(alpha = 0.92f))
            }
        }
        HudPill(Modifier.align(Alignment.BottomStart)) {
            FrameText(time, style = mono, color = Color.White.copy(alpha = 0.92f))
        }
    }
}

@Composable
private fun HudPill(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .clip(FrameShapes.pill)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), FrameShapes.pill)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun TraySlot(index: Int, filled: Int, frame: ImageBitmap?, hasClip: Boolean, modifier: Modifier) {
    val colors = FrameTheme.colors
    val shape = RoundedCornerShape(8.dp)
    val on = index < filled
    val reveal by animateFloatAsState(if (on) 1f else 0f, frameSpring(), label = "slot")
    val border = if (on) OnbColors.CyanSoft.copy(alpha = 0.6f) else frameHairline()
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(colors.containerHigh.copy(alpha = 0.5f))
            .border(1.dp, border, shape)
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                alpha = reveal
                scaleX = 0.85f + 0.15f * reveal
                scaleY = 0.85f + 0.15f * reveal
            }
        ) {
            when {
                !hasClip -> Canvas(Modifier.fillMaxSize()) { drawSplash(CaptureAt[index]) }
                frame != null -> Image(frame, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else -> Unit
            }
        }
    }
}

@Composable
private fun CounterRow(count: Int) {
    val colors = FrameTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountPill(count)
            FrameText(
                FrameStrings.onboardingInstant(),
                style = FrameTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = colors.onSurfaceVariant
            )
        }
        Box(Modifier.fillMaxWidth().height(3.dp).clip(FrameShapes.pill).background(colors.containerHigh.copy(alpha = 0.6f))) {
            Box(
                Modifier
                    .fillMaxWidth(count / TotalCount.toFloat())
                    .fillMaxHeight()
                    .clip(FrameShapes.pill)
                    .background(Brush.horizontalGradient(listOf(OnbColors.CyanSoft, OnbColors.Indigo, OnbColors.Violet)))
            )
        }
    }
}

@Composable
private fun CountPill(count: Int) {
    val colors = FrameTheme.colors
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier
                .clip(FrameShapes.pill)
                .background(colors.containerHigh.copy(alpha = 0.9f))
                .border(1.dp, frameHairline(), FrameShapes.pill)
                .padding(horizontal = 10.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(FrameShapes.pill).background(colors.success))
            FrameText(
                "$count / $TotalCount",
                style = FrameTheme.typography.monoSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = colors.success
            )
        }
    }
}
