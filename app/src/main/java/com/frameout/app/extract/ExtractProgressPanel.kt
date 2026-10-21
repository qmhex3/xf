package com.frameout.app.extract

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardBrush
import com.frameout.designsystem.theme.frameCardEdge

val LocalExtractProgress = compositionLocalOf<ExtractCapsuleState?> { null }

private const val RING_START = -90f
private const val RING_SWEEP_INDETERMINATE = 80f
private const val SPIN_MS = 1400

@Composable
fun ExtractProgressPanel(state: ExtractCapsuleState, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    Column(
        modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clip(FrameShapes.lg)
            .background(frameCardBrush(colors.primary, 0.12f), FrameShapes.lg)
            .border(1.dp, frameCardEdge(), FrameShapes.lg)
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .semantics { contentDescription = panelDescription(state) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProgressRing(state)
        FrameText(panelTitle(state), style = FrameTheme.typography.title, textAlign = TextAlign.Center)
        PanelStats(state)
        FrameText(QualityStrings.extractingHint(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

private fun panelTitle(state: ExtractCapsuleState): String = when {
    state.paused -> extractStatusLabel(state.status, state.message)
    state.status == "Queued" -> FrameStrings.queued()
    else -> QualityStrings.extractingTitle()
}

private fun panelDescription(state: ExtractCapsuleState): String = panelTitle(state) + " " + state.percent + "%"

@Composable
private fun ProgressRing(state: ExtractCapsuleState) {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val determinate = state.shownTotal > 0 || state.unified > 0f
    val target by animateFloatAsState(state.fraction, tween(400), label = "extractRing")
    val spin = if (determinate || reduce || state.paused) 0f else {
        val t = rememberInfiniteTransition(label = "extractSpin")
        val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(SPIN_MS, easing = LinearEasing)), label = "extractSpinAngle")
        angle
    }
    val track = colors.onSurface.copy(alpha = 0.09f)
    val arc = Brush.sweepGradient(listOf(colors.accentC, colors.accentA, colors.accentB, colors.accentC))
    Box(Modifier.size(132.dp), Alignment.Center) {
        Canvas(Modifier.size(132.dp)) {
            val stroke = Stroke(9.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2f
            val box = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), box, style = stroke)
            val sweep = if (determinate) 360f * target else RING_SWEEP_INDETERMINATE
            val start = if (determinate) RING_START else RING_START + spin
            if (sweep > 0.5f) drawArc(arc, start, sweep, false, Offset(inset, inset), box, style = stroke)
        }
        if (determinate) {
            FrameText("${state.percent}%", style = FrameTheme.typography.display.copy(textDirection = TextDirection.Ltr), color = colors.onSurface)
        } else {
            Image(painterResource(FrameIconsExtra.Frames), null, Modifier.size(34.dp), colorFilter = ColorFilter.tint(colors.primary))
        }
    }
}

@Composable
private fun PanelStats(state: ExtractCapsuleState) {
    val chips = buildList {
        if (state.counter.isNotEmpty()) add(PanelChip(FrameIconsExtra.Frames, state.counter, true))
        if (state.elapsed > 0L) add(PanelChip(FrameIconsExtra.Timer, FrameStrings.elapsed(formatExtractTime(state.elapsed)), false))
        if (state.remaining > 0L && !state.paused && !state.estimated) add(PanelChip(FrameIconsExtra.Timer, FrameStrings.remaining(formatExtractTime(state.remaining)), false))
    }
    if (chips.isEmpty()) return
    Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally) {
        chips.forEach { StatChip(it) }
    }
}

private class PanelChip(val icon: Int, val text: String, val accent: Boolean)

@Composable
private fun StatChip(chip: PanelChip) {
    val colors = FrameTheme.colors
    val tint = if (chip.accent) colors.primary else colors.onSurfaceVariant
    androidx.compose.foundation.layout.Row(
        Modifier.background(if (chip.accent) colors.primary.copy(alpha = 0.16f) else colors.containerHigh, FrameShapes.pill).padding(horizontal = 12.dp, vertical = 6.dp),
        Arrangement.spacedBy(6.dp),
        Alignment.CenterVertically
    ) {
        Image(painterResource(chip.icon), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(tint))
        FrameText(chip.text, style = FrameTheme.typography.label.copy(textDirection = if (chip.accent) TextDirection.Ltr else TextDirection.Content), color = tint, maxLines = 1)
    }
}
