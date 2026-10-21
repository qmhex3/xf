package com.frameout.app.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.glass.frameGlow
import kotlin.math.abs

private val CAPTURES = listOf(0.20f, 0.42f, 0.64f, 0.86f)
private const val FLASH_SPAN = 0.012f

private fun flashAt(p: Float): Float = CAPTURES.maxOf { (1f - abs(p - it) / FLASH_SPAN).coerceIn(0f, 1f) }

private fun badgeAt(p: Float): Float =
    CAPTURES.maxOf { ease(seg(p, it, it + 0.012f)) * (1f - seg(p, it + 0.07f, it + 0.10f)) }

@Composable
internal fun LiveReadyHero(clip: ClipHandle, width: Dp, modifier: Modifier = Modifier) {
    val frames = rememberClipFrames(clip.path, CAPTURES, 420, 216)
    val height = width * ReadyHeightFactor
    val cardWidth = (width * 0.8f).coerceAtMost(280.dp)
    val cardHeight = cardWidth * (144f / 280f)
    val ghost = DpSize(width * 0.32f, height * 0.77f)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.width(width).height(height).clearAndSetSemantics { }) {
            LiveGhost(frames, 0, 2, clip.phase, -1f, Modifier.align(Alignment.CenterStart).offset(x = 8.dp).size(ghost))
            LiveGhost(frames, 1, 3, clip.phase, 1f, Modifier.align(Alignment.CenterEnd).offset(x = (-8).dp).size(ghost))
            LiveCard(clip, Modifier.align(Alignment.Center).size(cardWidth, cardHeight))
        }
    }
}

@Composable
private fun LiveGhost(
    frames: List<androidx.compose.ui.graphics.ImageBitmap?>,
    first: Int,
    second: Int,
    phase: State<Float>,
    direction: Float,
    modifier: Modifier
) {
    val gray = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    val index by remember { derivedStateOf { if (phase.value >= CAPTURES[second]) second else first } }
    Box(
        modifier
            .graphicsLayer {
                val p = phase.value
                val at = if (p >= CAPTURES[second]) CAPTURES[second] else CAPTURES[first]
                val e = ease(seg(p, at, at + 0.04f))
                alpha = 0.30f * e
                translationX = direction * (1f - e) * 14.dp.toPx()
                scaleX = 0.9f
                scaleY = 0.9f
            }
            .clip(RoundedCornerShape(8.dp))
            .background(OnbColors.Ink)
    ) {
        frames.getOrNull(index)?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, colorFilter = gray) }
    }
}

@Composable
private fun LiveCard(clip: ClipHandle, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .frameGlow(com.frameout.designsystem.theme.FrameTheme.colors.primary, { 0.20f }, 22.dp, { 0.08f })
            .clip(shape)
            .background(Brush.verticalGradient(listOf(OnbColors.Ink, OnbColors.Night)))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    ) {
        ClipSurface(clip.player, Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) { drawLiveOverlay(clip.phase.value) }
        CapturedBadge(clip.phase, Modifier.align(Alignment.TopEnd).padding(10.dp), ::badgeAt)
        Timecode(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            clip.phase.value
            clip.player.currentPosition.toInt()
        }
    }
}

private fun DrawScope.drawLiveOverlay(p: Float) {
    val flash = flashAt(p)
    drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.50f))))
    val c = OnbColors.Ring.copy(alpha = 0.55f + 0.45f * flash)
    val inset = (14f - 4f * flash).dp.toPx()
    val len = 14.dp.toPx()
    val stroke = 1.5.dp.toPx()
    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        drawLine(c, Offset(x, y), Offset(x + dx * len, y), stroke, StrokeCap.Square)
        drawLine(c, Offset(x, y), Offset(x, y + dy * len), stroke, StrokeCap.Square)
    }
    corner(inset, inset, 1f, 1f)
    corner(size.width - inset, inset, -1f, 1f)
    corner(inset, size.height - inset, 1f, -1f)
    corner(size.width - inset, size.height - inset, -1f, -1f)
    if (flash > 0f) drawRect(Color.White.copy(alpha = 0.40f * flash))
}
