package com.frameout.app.trim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameRollingText
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import com.frameout.engine.extract.ExtractMode
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.ln

private const val METER_MAX_MB = 2048f
private const val METER_WARM_MB = 600f
private const val METER_HOT_MB = 1500f
private const val MB_PER_FRAME = 0.18f

internal fun estimateMbFromFrames(frames: Int): Float = frames * MB_PER_FRAME

internal fun planCount(count: Int): String = NumberFormat.getIntegerInstance().format(count.toLong())

internal fun planMegabytes(mb: Float): String = NumberFormat.getNumberInstance().apply { minimumFractionDigits = 1; maximumFractionDigits = 1 }.format(mb.toDouble())

private fun statUnit(text: String): String =
    text.replace(Regex("[\\p{Nd}\u2248\u066B\u066C,]"), "").trim { it == '.' || it.isWhitespace() }.ifBlank { text }

internal fun planFramesUnit(frames: Int): String = statUnit(TrimStrings.planFrames(frames))

internal fun planSizeUnit(): String = statUnit(TrimStrings.planSize("0"))

internal fun planVideosUnit(count: Int): String = statUnit(TrimStrings.batchVideos(count))

private fun stepFor(n: Int): Int = when {
    n <= 1 -> 1
    n == 2 -> 2
    n <= 4 -> 3
    n <= 10 -> 4
    else -> 6
}

internal fun pickPattern(mode: ExtractMode, cells: Int): BooleanArray {
    val out = BooleanArray(cells)
    fun mark(fraction: Float) { out[(fraction * (cells - 1)).toInt().coerceIn(0, cells - 1)] = true }
    when (mode) {
        ExtractMode.EveryFrame -> out.fill(true)
        is ExtractMode.EveryNFrames -> { var i = 0; val s = stepFor(mode.n); while (i < cells) { out[i] = true; i += s } }
        is ExtractMode.EveryNMillis -> { val s = (cells / 6).coerceAtLeast(2); var i = s / 2; while (i < cells) { out[i] = true; i += s } }
        is ExtractMode.FixedCount -> { val k = mode.count.coerceIn(2, 6); for (j in 0 until k) mark((j + 0.5f) / k) }
        ExtractMode.KeyframesOnly -> listOf(0.04f, 0.2f, 0.46f, 0.61f, 0.86f).forEach { mark(it) }
        ExtractMode.SceneChange -> listOf(0.2f, 0.47f, 0.74f).forEach { mark(it) }
        ExtractMode.Slides -> for (j in 0 until 3) out[(j * cells / 3).coerceIn(0, cells - 1)] = true
        is ExtractMode.Content -> listOf(0.12f, 0.41f, 0.58f, 0.9f).forEach { mark(it) }
    }
    return out
}

private class StripFrame(
    val mode: ExtractMode,
    val picked: BooleanArray,
    val cells: Int,
    val w: Float,
    val h: Float,
    val bandH: Float,
    val reveal: Float,
    val sweep: Float,
    val a: Color,
    val c: Color,
    val dim: Color
) {
    val cy = h / 2f
    val gap = w / cells
    val barW = (gap * 0.42f).coerceAtLeast(1.5f)
    val slides = mode == ExtractMode.Slides
}

@Composable
internal fun PlanStrip(mode: ExtractMode, modifier: Modifier, cells: Int, perforations: Boolean) {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val picked = remember(mode, cells) { pickPattern(mode, cells) }
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(mode, reduce) {
        if (reduce) reveal.snapTo(1f) else {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
        }
    }
    val sweep: State<Float> = if (reduce) remember { mutableStateOf(0.5f) } else rememberInfiniteTransition(label = "planSweep").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing)),
        label = "planSweepX"
    )
    val a = colors.accentA
    val c = colors.accentC
    val dim = colors.onSurfaceVariant
    Canvas(modifier) {
        val bandH = size.height - (if (perforations) size.height * 0.25f else 0f) * 2f
        val frame = StripFrame(mode, picked, cells, size.width, size.height, bandH, reveal.value, sweep.value, a, c, dim)
        drawStripSweep(frame)
        for (i in 0 until cells) drawStripCell(frame, i)
        if (perforations) drawStripPerforations(frame)
    }
}

private fun DrawScope.drawStripSweep(f: StripFrame) {
    val sx = f.sweep * f.w
    val sr = f.gap * 4f
    drawRect(
        Brush.horizontalGradient(listOf(Color.Transparent, f.a.copy(alpha = 0.16f), Color.Transparent), startX = sx - sr, endX = sx + sr),
        topLeft = Offset(sx - sr, 0f),
        size = Size(sr * 2f, f.h)
    )
}

private fun DrawScope.drawStripCell(f: StripFrame, i: Int) {
    val t = i / (f.cells - 1).coerceAtLeast(1).toFloat()
    val cx = f.gap * (i + 0.5f)
    val near = (1f - abs(cx / f.w - f.sweep) * 4.5f).coerceIn(0f, 1f)
    if (!f.picked[i]) {
        drawStripIdle(f, cx, near)
        return
    }
    val p = ((f.reveal - t * 0.55f) / 0.45f).coerceIn(0f, 1f)
    val col = lerp(f.a, f.c, t)
    val alpha = (0.72f + 0.28f * near) * (0.3f + 0.7f * p)
    when (f.mode) {
        ExtractMode.KeyframesOnly -> drawStripKeyframe(f, cx, col, alpha, p)
        ExtractMode.SceneChange -> drawStripScene(f, cx, col, alpha, p)
        ExtractMode.Slides -> drawStripSlide(f, cx, col, alpha, p, near)
        else -> drawStripBar(f, cx, col, alpha, p)
    }
}

private fun DrawScope.drawStripIdle(f: StripFrame, cx: Float, near: Float) {
    if (f.slides) return
    val hh = f.bandH * 0.26f
    drawRoundRect(f.dim.copy(alpha = 0.18f + 0.30f * near), Offset(cx - f.barW * 0.4f, f.cy - hh / 2f), Size(f.barW * 0.8f, hh), CornerRadius(f.barW))
}

private fun DrawScope.drawStripKeyframe(f: StripFrame, cx: Float, col: Color, alpha: Float, p: Float) {
    val d = (f.bandH * 0.36f * p).coerceAtLeast(0.1f)
    drawLine(col.copy(alpha = 0.25f * p), Offset(cx, f.cy - f.bandH / 2f), Offset(cx, f.cy + f.bandH / 2f), strokeWidth = f.barW * 0.5f, cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(cx, f.cy - d); lineTo(cx + d, f.cy); lineTo(cx, f.cy + d); lineTo(cx - d, f.cy); close()
    }
    drawPath(path, col.copy(alpha = alpha))
}

private fun DrawScope.drawStripScene(f: StripFrame, cx: Float, col: Color, alpha: Float, p: Float) {
    val half = f.bandH / 2f * p
    drawLine(col.copy(alpha = alpha), Offset(cx, f.cy - half), Offset(cx, f.cy + half), strokeWidth = f.barW * 0.7f, cap = StrokeCap.Round)
    drawCircle(col.copy(alpha = alpha), f.barW * 1.1f * p, Offset(cx, f.cy))
}

private fun DrawScope.drawStripSlide(f: StripFrame, cx: Float, col: Color, alpha: Float, p: Float, near: Float) {
    val left = (cx - f.gap / 2f).coerceAtLeast(0f)
    val bw = minOf(f.gap * (f.cells / 3f) * 0.86f, f.w - left)
    val bh = f.bandH * 0.84f * p
    val topLeft = Offset(left, f.cy - bh / 2f)
    val corner = CornerRadius(f.gap * 0.8f)
    drawRoundRect(col.copy(alpha = 0.18f * p + 0.10f * near), topLeft, Size(bw, bh), corner)
    drawRoundRect(col.copy(alpha = alpha), topLeft, Size(bw, bh), corner, style = Stroke(width = 1.5.dp.toPx()))
    val line = 2.dp.toPx()
    drawRoundRect(col.copy(alpha = 0.55f * p), Offset(left + f.gap * 0.9f, f.cy - bh * 0.16f), Size(bw * 0.5f, line), CornerRadius(line))
    drawRoundRect(col.copy(alpha = 0.35f * p), Offset(left + f.gap * 0.9f, f.cy + bh * 0.06f), Size(bw * 0.32f, line), CornerRadius(line))
}

private fun DrawScope.drawStripBar(f: StripFrame, cx: Float, col: Color, alpha: Float, p: Float) {
    val hh = f.bandH * (0.34f + 0.62f * p)
    drawRoundRect(col.copy(alpha = 0.16f * p), Offset(cx - f.barW * 1.5f, f.cy - hh / 2f - 2f), Size(f.barW * 3f, hh + 4f), CornerRadius(f.barW * 1.5f))
    drawRoundRect(col.copy(alpha = alpha), Offset(cx - f.barW / 2f, f.cy - hh / 2f), Size(f.barW, hh), CornerRadius(f.barW))
}

private fun DrawScope.drawStripPerforations(f: StripFrame) {
    val pw = f.h * 0.09f
    val ph = f.h * 0.11f
    val pg = f.h * 0.34f
    val hole = f.dim.copy(alpha = 0.30f)
    var x = pg / 2f
    while (x < f.w) {
        drawRoundRect(hole, Offset(x - pw / 2f, f.h * 0.07f), Size(pw, ph), CornerRadius(pw * 0.3f))
        drawRoundRect(hole, Offset(x - pw / 2f, f.h - f.h * 0.07f - ph), Size(pw, ph), CornerRadius(pw * 0.3f))
        x += pg
    }
}

@Composable
internal fun PlanFilmStrip(mode: ExtractMode, modifier: Modifier = Modifier, height: Dp = 56.dp) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.sm
    val well = if (colors.isDark) Color.Black.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.07f)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(well).border(1.dp, frameHairline(), shape)) {
        PlanStrip(mode, Modifier.fillMaxSize(), cells = 30, perforations = true)
    }
}

@Composable
internal fun PlanMeter(megabytes: Float, modifier: Modifier) {
    val colors = FrameTheme.colors
    val target = (ln(1f + megabytes) / ln(1f + METER_MAX_MB)).coerceIn(0.03f, 1f)
    val fill by animateFloatAsState(target, tween(550, easing = FastOutSlowInEasing), label = "planMeter")
    val a = colors.accentA
    val c = colors.accentC
    val warm = colors.warning
    val hot = colors.error
    val track = colors.onSurface.copy(alpha = 0.08f)
    Canvas(modifier) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(track, cornerRadius = radius)
        val fw = (size.width * fill).coerceAtLeast(1f)
        val pair = when {
            megabytes > METER_HOT_MB -> listOf(warm, hot)
            megabytes > METER_WARM_MB -> listOf(a, warm)
            else -> listOf(a, c)
        }
        drawRoundRect(Brush.horizontalGradient(pair, startX = 0f, endX = fw), size = Size(fw, size.height), cornerRadius = radius)
    }
}

@Composable
internal fun PlanStat(icon: Int, value: String, unit: String, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.sm
    Column(
        modifier
            .clip(shape)
            .background(colors.onSurface.copy(alpha = 0.05f))
            .border(1.dp, frameHairline(), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
            Image(painterResource(icon), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.primary))
            FrameText(unit, style = FrameTheme.typography.label, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FrameRollingText(value, FrameTheme.typography.title, colors.onSurface)
        }
    }
}
