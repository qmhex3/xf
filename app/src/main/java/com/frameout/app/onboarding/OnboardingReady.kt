package com.frameout.app.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import java.util.Locale
import kotlin.math.abs

internal const val ReadyHeightFactor = 0.5f

internal val ReadyExtraHeight = 0.dp

private val ReadyAt = listOf(0.40f, 0.50f, 0.60f)

@Composable
internal fun ReadyHero(width: Dp, modifier: Modifier = Modifier) {
    val clip = rememberClip(OnboardingClips.READY)
    if (clip != null) LiveReadyHero(clip, width, modifier) else StillReadyHero(width, modifier)
}

@Composable
private fun StillReadyHero(width: Dp, modifier: Modifier) {
    val path = rememberFirstClip(OnboardingClips.PICK, OnboardingClips.FORMATS, OnboardingClips.HERO)
    val phase = rememberLoop(durationMs = 6000, still = 0.9f, reverse = false, label = "ready")
    val frames = if (path != null) rememberClipFrames(path, ReadyAt, 420, 216) else null
    val height = width * ReadyHeightFactor
    val cardWidth = (width * 0.8f).coerceAtMost(280.dp)
    val cardHeight = cardWidth * (144f / 280f)
    val ghostWidth = width * 0.32f
    val ghostHeight = height * 0.77f
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.width(width).height(height).clearAndSetSemantics { }) {
            GhostFrame(frames, 0, phase, -1f, Modifier.align(Alignment.CenterStart).offset(x = 8.dp).size(ghostWidth, ghostHeight))
            GhostFrame(frames, 2, phase, 1f, Modifier.align(Alignment.CenterEnd).offset(x = (-8).dp).size(ghostWidth, ghostHeight))
            CaptureFrame(frames, phase, Modifier.align(Alignment.Center).size(cardWidth, cardHeight))
        }
    }
}

@Composable
private fun GhostFrame(frames: List<ImageBitmap?>?, index: Int, phase: State<Float>, direction: Float, modifier: Modifier) {
    val gray = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    Box(
        modifier
            .graphicsLayer {
                val p = phase.value
                val e = ease(seg(p, 0.02f, 0.15f))
                alpha = 0.22f * e * (1f - seg(p, 0.96f, 1f))
                translationX = direction * (1f - e) * 14.dp.toPx()
                scaleX = 0.9f
                scaleY = 0.9f
            }
            .clip(RoundedCornerShape(8.dp))
            .background(OnbColors.Ink)
    ) {
        val frame = frames?.getOrNull(index)
        when {
            frames == null -> Canvas(Modifier.fillMaxSize()) { drawSplash(ReadyAt[index]) }
            frame != null -> Image(frame, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, colorFilter = gray)
            else -> Unit
        }
    }
}

@Composable
private fun CaptureFrame(frames: List<ImageBitmap?>?, phase: State<Float>, modifier: Modifier) {
    val colors = FrameTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .frameGlow(colors.primary, { 0.20f }, 22.dp, { 0.08f })
            .clip(shape)
            .background(Brush.verticalGradient(listOf(OnbColors.Ink, OnbColors.Night)))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    ) {
        val frame = frames?.getOrNull(1)
        when {
            frames == null -> Canvas(Modifier.fillMaxSize()) { drawSplash(ReadyAt[1]) }
            frame != null -> Image(frame, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else -> Unit
        }
        Canvas(Modifier.fillMaxSize()) { drawCaptureOverlay(phase.value) }
        CapturedBadge(phase, Modifier.align(Alignment.TopEnd).padding(10.dp))
        Timecode(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            (12000f + 960f * ease(seg(phase.value, 0.10f, 0.50f))).toInt()
        }
    }
}

private fun DrawScope.drawCaptureOverlay(p: Float) {
    val out = 1f - seg(p, 0.96f, 1f)
    drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.30f), Color.Transparent, Color.Black.copy(alpha = 0.60f))))
    val line = ease(seg(p, 0.05f, 0.25f)) * out
    drawLine(
        brush = Brush.horizontalGradient(listOf(Color.Transparent, OnbColors.CyanSoft.copy(alpha = 0.9f * line), Color.Transparent)),
        start = Offset(0f, size.height / 2f),
        end = Offset(size.width, size.height / 2f),
        strokeWidth = 1.dp.toPx()
    )
    val close = ease(seg(p, 0.10f, 0.45f))
    val inset = (22f - 12f * close).dp.toPx()
    val len = 12.dp.toPx()
    val c = OnbColors.Ring.copy(alpha = (0.4f + 0.6f * close) * out)
    val stroke = 1.5.dp.toPx()
    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        drawLine(c, Offset(x, y), Offset(x + dx * len, y), stroke, StrokeCap.Square)
        drawLine(c, Offset(x, y), Offset(x, y + dy * len), stroke, StrokeCap.Square)
    }
    corner(inset, inset, 1f, 1f)
    corner(size.width - inset, inset, -1f, 1f)
    corner(inset, size.height - inset, 1f, -1f)
    corner(size.width - inset, size.height - inset, -1f, -1f)
    val flash = (1f - abs(p - 0.5f) / 0.04f).coerceIn(0f, 1f)
    if (flash > 0f) drawRect(Color.White.copy(alpha = 0.45f * flash))
}

@Composable
internal fun CapturedBadge(
    phase: State<Float>,
    modifier: Modifier,
    level: (Float) -> Float = { p -> ease(seg(p, 0.52f, 0.58f)) * (1f - seg(p, 0.94f, 1f)) }
) {
    Row(
        modifier
            .graphicsLayer {
                val b = level(phase.value)
                alpha = b
                scaleX = 0.8f + 0.2f * b
                scaleY = scaleX
            }
            .clip(FrameShapes.pill)
            .background(Color(0xFF0D0E14).copy(alpha = 0.75f))
            .border(1.dp, OnbColors.Cyan.copy(alpha = 0.35f), FrameShapes.pill)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(FrameIcons.Check), null, Modifier.size(12.dp), colorFilter = ColorFilter.tint(OnbColors.CyanSoft))
        FrameText(
            FrameStrings.onboardingCaptured(),
            style = FrameTheme.typography.titleSmall.copy(fontSize = 11.sp),
            color = OnbColors.CyanSoft,
            maxLines = 1
        )
    }
}

@Composable
internal fun Timecode(modifier: Modifier, millis: () -> Int) {
    val ms by remember { derivedStateOf(millis) }
    FrameText(
        String.format(Locale.US, "00:00:%02d.%03d", ms / 1000, ms % 1000),
        style = FrameTheme.typography.monoSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
        color = OnbColors.CyanSoft,
        maxLines = 1,
        modifier = modifier
            .clip(FrameShapes.xs)
            .background(Color(0xFF0D0E14).copy(alpha = 0.8f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), FrameShapes.xs)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
