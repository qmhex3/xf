package com.frameout.app.batch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.app.trim.ExtractOptions
import com.frameout.app.trim.PlanChangeButton
import com.frameout.app.trim.PlanFilmStrip
import com.frameout.app.trim.PlanMeter
import com.frameout.app.trim.PlanStat
import com.frameout.app.trim.estimateMbFromFrames
import com.frameout.app.trim.planCount
import com.frameout.app.trim.planFramesUnit
import com.frameout.app.trim.planMegabytes
import com.frameout.app.trim.planSizeUnit
import com.frameout.app.trim.planVideosUnit
import com.frameout.app.trim.TrimSheetHost
import com.frameout.app.trim.estimateFrames
import com.frameout.app.trim.planText
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameSpinner
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.FrameTopBar
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameAmbientGlow
import com.frameout.designsystem.theme.frameCardBrush
import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.designsystem.theme.frameHairline
import androidx.compose.ui.graphics.SolidColor
import com.frameout.engine.extract.ExtractModeCodec
import com.frameout.engine.timeline.TrimRange
import com.frameout.engine.timeline.formatTimestamp

private val FULL = TrimRange()

@Composable
internal fun BatchPrepare(
    items: List<PrepItem>,
    starting: Boolean,
    prefs: AppPrefs,
    onBack: () -> Unit,
    onEdit: (PrepItem) -> Unit,
    onRemove: (String) -> Unit,
    onStart: () -> Unit,
    hasSpace: (Int) -> Boolean
) {
    val stored by prefs.extractMode.collectAsState()
    val autoSave by prefs.autoSave.collectAsState()
    var options by remember { mutableStateOf(false) }
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(items.isEmpty()) {
        if (items.isNotEmpty()) seen = true else if (seen) onBack()
    }
    val total = items.sumOf { estimateFrames(it.durationMs, it.range ?: FULL, stored, it.probe) }
    val roomy = remember(total, autoSave) { hasSpace(total) }
    Box(Modifier.fillMaxSize().background(FrameTheme.colors.background).frameAmbientGlow()) {
        Column(Modifier.fillMaxSize()) {
            FrameTopBar(title = TrimStrings.batchPrepareTitle(), onBack = onBack, backContentDescription = FrameStrings.back())
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "settings") {
                    Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        BatchSettingsCard(items.size, total, stored, tappable = true) { options = true }
                        if (autoSave) Hint(TrimStrings.galleryWarn(total), error = true)
                        if (!roomy) Hint(FrameStrings.lowStorage(), error = true)
                    }
                }
                item(key = "header") {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), Arrangement.spacedBy(2.dp)) {
                        FrameText(TrimStrings.batchVideosTitle(items.size), style = FrameTheme.typography.titleSmall)
                        FrameText(TrimStrings.batchTapHint(), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant)
                    }
                }
                items(items, key = { it.uri }) { item ->
                    PrepRow(item, stored, { onEdit(item) }, { onRemove(item.uri) })
                }
            }
            StartBar(items.size, total, enabled = items.isNotEmpty() && roomy && !starting, onClick = onStart)
        }
        TrimSheetHost(options, TrimStrings.optionsTitle(), { options = false }) {
            ExtractOptions(
                prefs, autoSave, { prefs.setAutoSave(!autoSave) }, total,
                summary = { BatchSettingsCard(items.size, total, stored, tappable = false) {} },
                onDone = { options = false }
            )
        }
        if (starting) Scrim()
    }
}

@Composable
private fun BoxScope.Scrim() {
    Box(
        Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)).frameClickable(onClick = {}),
        Alignment.Center
    ) { FrameSpinner() }
}

@Composable
private fun Hint(text: String, error: Boolean) {
    FrameText(
        text,
        style = FrameTheme.typography.bodySmall,
        color = if (error) FrameTheme.colors.error else FrameTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun BatchSettingsCard(count: Int, total: Int, stored: String, tappable: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val mode = ExtractModeCodec.decode(stored)
    val megabytes = estimateMbFromFrames(total)
    val click = if (tappable) Modifier.frameClickable(onClick = onClick) else Modifier
    val shape = FrameShapes.xl
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(frameCardBrush(colors.accentA, 0.14f), shape)
            .border(1.dp, frameCardEdge(colors.accentA, 0.5f), shape)
            .then(click)
            .padding(14.dp),
        Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
            Column(Modifier.weight(1f), Arrangement.spacedBy(2.dp)) {
                FrameText(TrimStrings.batchSettingsTitle(), style = FrameTheme.typography.label, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                FrameText(planText(mode), style = FrameTheme.typography.titleSmall, maxLines = 2)
            }
            if (tappable) PlanChangeButton()
        }
        PlanFilmStrip(mode)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            PlanStat(FrameIconsExtra.Clip, planCount(count), planVideosUnit(count), Modifier.weight(1f))
            PlanStat(FrameIconsExtra.Frames, planCount(total), planFramesUnit(total), Modifier.weight(1f))
            PlanStat(FrameIconsExtra.Storage, planMegabytes(megabytes), planSizeUnit(), Modifier.weight(1f))
        }
        PlanMeter(megabytes, Modifier.fillMaxWidth().height(4.dp))
        FrameText(TrimStrings.batchSettingsHint(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Chip(text: String, accent: Boolean = false, icon: Int? = null) {
    val colors = FrameTheme.colors
    Row(
        Modifier.background(if (accent) colors.primary.copy(alpha = 0.16f) else colors.containerHigh, FrameShapes.pill).padding(horizontal = 10.dp, vertical = 4.dp),
        Arrangement.spacedBy(4.dp),
        Alignment.CenterVertically
    ) {
        icon?.let { Image(painterResource(it), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.primary)) }
        FrameText(
            text,
            style = FrameTheme.typography.label.copy(textDirection = if (accent) TextDirection.Ltr else TextDirection.Content),
            color = if (accent) colors.primary else colors.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun PrepRow(item: PrepItem, stored: String, onEdit: () -> Unit, onRemove: () -> Unit) {
    val colors = FrameTheme.colors
    val frames = estimateFrames(item.durationMs, item.range ?: FULL, stored, item.probe)
    val edge: Brush = if (item.edited) SolidColor(colors.primary.copy(alpha = 0.45f)) else frameCardEdge()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FrameShapes.md)
            .background(frameCardBrush())
            .border(1.dp, edge, FrameShapes.md)
            .frameClickable(onClick = onEdit)
            .semantics { contentDescription = TrimStrings.batchEdit() + ": " + item.name }
            .padding(10.dp),
        Arrangement.spacedBy(12.dp),
        Alignment.CenterVertically
    ) {
        Thumb(item)
        Column(Modifier.weight(1f), Arrangement.spacedBy(8.dp)) {
            FrameText(item.name, style = FrameTheme.typography.titleSmall.copy(textDirection = TextDirection.Content), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.edited) {
                val r = item.range!!
                Chip(formatTimestamp(r.startMs) + " – " + formatTimestamp(r.endMs), accent = true, icon = FrameIconsExtra.TrimRange)
            } else {
                Chip(TrimStrings.wholeVideo())
            }
            FrameText(TrimStrings.planFrames(frames), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Column(Modifier.height(THUMB_HEIGHT), Arrangement.SpaceBetween) {
            RoundAction(FrameIconsExtra.Edit, TrimStrings.batchEdit(), true, onEdit)
            RoundAction(FrameIcons.Close, TrimStrings.batchRemove(), false, onRemove)
        }
    }
}
