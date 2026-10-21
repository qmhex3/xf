package com.frameout.app.batch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameSpinner
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameCardBrush
import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.designsystem.theme.frameHairline

@Composable
internal fun ProgressTrack(fraction: Float, modifier: Modifier = Modifier, brush: Brush = FrameGradients.Progress) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "batchProgress")
    Box(modifier.height(6.dp).clip(FrameShapes.pill).background(FrameTheme.colors.outline)) {
        Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(FrameShapes.pill).background(brush))
    }
}

@Composable
internal fun BatchSummary(state: BatchState) {
    val colors = FrameTheme.colors
    val percent = (state.progress * 100).toInt()
    Column(
        Modifier
            .fillMaxWidth()
            .background(frameCardBrush(), FrameShapes.lg)
            .border(1.dp, frameCardEdge(), FrameShapes.lg)
            .padding(18.dp),
        Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
            Column(Modifier.weight(1f), Arrangement.spacedBy(3.dp)) {
                FrameText(FrameStrings.finished(state.completedCount, state.items.size), style = FrameTheme.typography.titleSmall)
                state.active?.let {
                    FrameText(it.name, style = FrameTheme.typography.bodySmall.copy(textDirection = TextDirection.Content), color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            FrameText(
                "$percent%",
                style = FrameTheme.typography.display.copy(textDirection = TextDirection.Ltr),
                color = if (state.done) colors.success else colors.primary
            )
        }
        ProgressTrack(state.progress, Modifier.fillMaxWidth())
    }
}

@Composable
internal fun BatchRow(item: BatchItem, onOpen: () -> Unit) {
    val colors = FrameTheme.colors
    val canOpen = item.status == "Completed" || item.extracted > 0
    val accent = statusColor(item.status)
    Row(
        Modifier
            .fillMaxWidth()
            .background(frameCardBrush(), FrameShapes.md)
            .border(1.dp, frameCardEdge(), FrameShapes.md)
            .then(if (canOpen) Modifier.frameClickable(onClick = onOpen) else Modifier)
            .padding(14.dp),
        Arrangement.spacedBy(14.dp),
        Alignment.CenterVertically
    ) {
        StatusBadge(item.status, accent)
        Column(Modifier.weight(1f), Arrangement.spacedBy(8.dp)) {
            FrameText(item.name, style = FrameTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!item.finished && item.total > 0) ProgressTrack(item.fraction, Modifier.fillMaxWidth())
            FrameText(statusLine(item), style = FrameTheme.typography.bodySmall, color = accent)
        }
        if (!item.finished && item.total > 0) {
            FrameText(
                "${(item.fraction * 100).toInt()}%",
                style = FrameTheme.typography.label.copy(textDirection = TextDirection.Ltr),
                color = accent
            )
        }
    }
}

private fun statusLine(item: BatchItem): String =
    FrameStrings.status(item.status) + " · ${item.extracted}" + if (item.total > 0) "/${item.total}" else ""

@Composable
private fun statusColor(status: String): Color {
    val colors = FrameTheme.colors
    return when (status) {
        "Completed" -> colors.success
        "Failed", "Cancelled" -> colors.error
        "Extracting", "Indexing" -> colors.primary
        else -> colors.onSurfaceVariant
    }
}

@Composable
private fun StatusBadge(status: String, accent: Color) {
    val icon = when (status) {
        "Completed" -> FrameIcons.Check
        "Failed", "Cancelled" -> FrameIconsExtra.Warning
        "Extracting", "Indexing" -> FrameIconsExtra.Live
        "Paused" -> FrameIcons.Pause
        else -> FrameIconsExtra.Timer
    }
    Box(Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), Alignment.Center) {
        if (status == "Extracting" || status == "Indexing") {
            FrameSpinner(size = 22.dp, strokeWidth = 2.5.dp, color = accent)
        } else {
            Image(painterResource(icon), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(accent))
        }
    }
}

@Composable
internal fun BatchBottomBar(done: Boolean, onDone: () -> Unit, onCancel: () -> Unit) {
    val colors = FrameTheme.colors
    val label = if (done) FrameStrings.done() else FrameStrings.cancel()
    val fill: Brush = if (done) FrameGradients.Brand else SolidColor(colors.error.copy(alpha = 0.12f))
    val edge: Brush = if (done) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.06f))) else SolidColor(colors.error.copy(alpha = 0.45f))
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .frameGlow(colors.primary, { if (done) 0.3f else 0f }, 14.dp)
                .clip(FrameShapes.pill)
                .background(fill, FrameShapes.pill)
                .border(1.dp, edge, FrameShapes.pill)
                .frameClickable(onClick = if (done) onDone else onCancel),
            Alignment.Center
        ) {
            FrameText(label, style = FrameTheme.typography.titleSmall, color = if (done) Color.White else colors.error)
        }
    }
}
