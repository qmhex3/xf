package com.frameout.app.frames

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.frameout.core.quality.TagNames
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private const val STRIP_LIMIT = 12

@Composable
fun FramesTagStrip(state: FramesState, onToggle: (String) -> Unit, onClear: () -> Unit, onAll: () -> Unit) {
    val lang = com.frameout.core.util.AppLang.of(LocalConfiguration.current.locales[0].language)
    val visibleTags = remember(state.tagCounts, state.tagFilter) {
        val selected = state.tagCounts.filter { it.tag in state.tagFilter }
        selected + state.tagCounts.filter { it.tag !in state.tagFilter }.take((STRIP_LIMIT - selected.size).coerceAtLeast(0))
    }
    val show = state.tagCounts.isNotEmpty() || state.scanning
    AnimatedVisibility(show) {
        LazyRow(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.scanning) item(key = "scan") { ScanChip(state.scanDone, state.scanTotal, state.scanFailed) }
            if (state.tagFilter.isNotEmpty()) item(key = "clear") { ClearChip(onClear) }
            items(visibleTags, key = { it.tag }) { t ->
                TagChip(TagNames.display(t.tag, lang), t.frames, t.tag in state.tagFilter) { onToggle(t.tag) }
            }
            if (state.tagCounts.isNotEmpty()) item(key = "all") { AllChip(onAll) }
        }
    }
}

@Composable
internal fun TagChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .clip(FrameShapes.pill)
            .background(if (selected) colors.primaryContainer else colors.container)
            .border(1.dp, if (selected) colors.primary.copy(alpha = 0.5f) else frameHairline(), FrameShapes.pill)
            .frameSelectable(selected, role = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FrameText(label, style = FrameTheme.typography.label, color = if (selected) colors.primary else colors.onSurface, maxLines = 1)
        FrameText(count.toString(), style = FrameTheme.typography.monoSmall, color = if (selected) colors.primary else colors.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun ScanChip(done: Int, total: Int, failed: Int) {
    val colors = FrameTheme.colors
    Row(
        Modifier.clip(FrameShapes.pill).background(colors.primary.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(6.dp).background(colors.primary, FrameShapes.pill))
        FrameText(QualityStrings.contentScanProgress(done, total, failed), style = FrameTheme.typography.label, color = colors.primary, maxLines = 1)
    }
}

@Composable
private fun ClearChip(onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        Modifier.clip(FrameShapes.pill).border(1.dp, colors.error.copy(alpha = 0.5f), FrameShapes.pill).frameClickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)
    ) { FrameText(QualityStrings.contentClear(), style = FrameTheme.typography.label, color = colors.error, maxLines = 1) }
}

@Composable
private fun AllChip(onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        Modifier.clip(FrameShapes.pill).border(1.dp, colors.primary.copy(alpha = 0.4f), FrameShapes.pill).frameClickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)
    ) { FrameText(QualityStrings.tagsAll(), style = FrameTheme.typography.label, color = colors.primary, maxLines = 1) }
}
