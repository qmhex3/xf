package com.frameout.app.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

internal const val Cells = 9
private const val ApexIndex = 4
private val DuplicateCells = setOf(0, 1)
internal val BlurCells = setOf(2, 7)

internal val BurstAt = List(Cells) { 0.38f + 0.03f * it }

private val Ring = Color(0xFF8E91FF)
internal val OnDark = Color(0xFFC7C4D7)

internal fun seg(p: Float, from: Float, to: Float): Float = ((p - from) / (to - from)).coerceIn(0f, 1f)
internal fun ease(x: Float): Float = x * x * (3f - 2f * x)

private fun tagStart(index: Int): Float = when (index) {
    in DuplicateCells -> 0.36f
    2 -> 0.40f
    7 -> 0.44f
    else -> 2f
}

private const val LiftFrom = 0.64f
private const val LiftTo = 0.74f

internal fun DrawScope.drawSparkle(color: Color) {
    val w = size.width
    val h = size.height
    val star = Path().apply {
        moveTo(w * 0.5f, 0f)
        lineTo(w * 0.60f, h * 0.40f)
        lineTo(w, h * 0.5f)
        lineTo(w * 0.60f, h * 0.60f)
        lineTo(w * 0.5f, h)
        lineTo(w * 0.40f, h * 0.60f)
        lineTo(0f, h * 0.5f)
        lineTo(w * 0.40f, h * 0.40f)
        close()
    }
    drawPath(star, color)
}

@Composable
internal fun BurstCell(
    index: Int,
    frames: List<ImageBitmap?>?,
    blurred: Map<Int, ImageBitmap?>?,
    phase: State<Float>,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val apex = index == ApexIndex
    val flagged = index in DuplicateCells || index in BlurCells
    val start = tagStart(index)
    val glow = if (apex) {
        Modifier.frameGlow(Ring, { 0.55f * ease(seg(phase.value, LiftFrom, LiftTo)) }, 14.dp, { 0.1f })
    } else {
        Modifier
    }
    Box(
        modifier
            .aspectRatio(4f / 3f)
            .graphicsLayer {
                val p = phase.value
                val appear = ease(seg(p, 0.02f + 0.03f * index, 0.07f + 0.03f * index)) * (1f - seg(p, 0.96f, 1f))
                val lift = if (apex) ease(seg(p, LiftFrom, LiftTo)) else 0f
                alpha = appear
                scaleX = (0.9f + 0.1f * appear) * (1f + 0.08f * lift)
                scaleY = scaleX
            }
            .then(glow)
    ) {
        Box(Modifier.fillMaxSize().clip(shape).background(OnbColors.Ink).border(1.dp, Color.White.copy(alpha = 0.06f), shape)) {
            val frame = frames?.getOrNull(index)
            when {
                frames == null -> Canvas(Modifier.fillMaxSize()) { drawSplash(BurstAt[index]) }
                frame != null -> {
                    Image(frame, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    blurred?.get(index)?.let { soft ->
                        Image(
                            soft,
                            null,
                            Modifier.fillMaxSize().graphicsLayer { alpha = ease(seg(phase.value, start, start + 0.04f)) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                else -> Unit
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = phase.value
                        alpha = when {
                            flagged -> 0.25f + 0.25f * seg(p, start, start + 0.04f)
                            apex -> 0.25f * (1f - seg(p, LiftFrom, LiftTo))
                            else -> 0.25f
                        }
                    }
                    .background(Color.Black)
            )
            if (flagged) {
                CellTag(
                    blur = index in BlurCells,
                    progress = { seg(phase.value, start, start + 0.04f) },
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                )
            }
            if (apex) Canvas(Modifier.fillMaxSize()) { drawApexOverlay(phase.value) }
        }
        if (index == 0) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 6.dp)
                    .size(width = 6.dp, height = 2.dp)
                    .graphicsLayer { alpha = seg(phase.value, start, start + 0.04f) }
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
        if (apex) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .graphicsLayer {
                        val b = ease(seg(phase.value, 0.72f, 0.78f))
                        alpha = b
                        scaleX = 0.8f + 0.2f * b
                        scaleY = scaleX
                    }
                    .clip(FrameShapes.pill)
                    .background(Brush.horizontalGradient(listOf(OnbColors.CyanSoft, Ring)))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(Modifier.size(9.dp)) { drawSparkle(Color(0xFF002E38)) }
                FrameText(
                    FrameStrings.sharpest(),
                    style = FrameTheme.typography.titleSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF002E38),
                    maxLines = 1
                )
            }
        }
    }
}

private fun DrawScope.drawApexOverlay(p: Float) {
    val lift = ease(seg(p, LiftFrom, LiftTo))
    drawRoundRect(
        color = Ring.copy(alpha = lift),
        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
        size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
        cornerRadius = CornerRadius(10.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )
    val inset = 5.dp.toPx()
    val len = 8.dp.toPx()
    val c = Ring.copy(alpha = lift)
    val stroke = 2.dp.toPx()
    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        drawLine(c, Offset(x, y), Offset(x + dx * len, y), stroke, StrokeCap.Round)
        drawLine(c, Offset(x, y), Offset(x, y + dy * len), stroke, StrokeCap.Round)
    }
    corner(inset, inset, 1f, 1f)
    corner(size.width - inset, inset, -1f, 1f)
    corner(inset, size.height - inset, 1f, -1f)
    corner(size.width - inset, size.height - inset, -1f, -1f)

    val sweep = ease(seg(p, 0.52f, 0.66f))
    val visible = if (p < 0.52f) 0f else if (p < 0.66f) 1f else 0.8f
    if (visible > 0f) {
        val y = size.height * 0.5f * sweep
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, OnbColors.CyanSoft.copy(alpha = visible), Color.Transparent)),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

@Composable
private fun CellTag(blur: Boolean, progress: () -> Float, modifier: Modifier) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.xs
    val fill = if (blur) colors.error.copy(alpha = 0.22f) else Color(0xFF121319).copy(alpha = 0.8f)
    val edge = if (blur) colors.error.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.18f)
    FrameText(
        if (blur) FrameStrings.onboardingTagBlurry() else FrameStrings.onboardingTagDuplicate(),
        style = FrameTheme.typography.titleSmall.copy(fontSize = 10.sp),
        color = if (blur) colors.error else OnDark,
        maxLines = 1,
        modifier = modifier
            .graphicsLayer { alpha = progress() }
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}
