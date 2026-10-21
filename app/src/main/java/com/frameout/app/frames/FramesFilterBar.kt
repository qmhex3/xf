package com.frameout.app.frames

import androidx.compose.ui.draw.clip
import com.frameout.designsystem.theme.frameControl
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.a11y.frameClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun FramesFilterBar(
    current: FrameFilter,
    onSelect: (FrameFilter) -> Unit,
    onSharpest: () -> Unit,
    onDuplicates: () -> Unit,
    onSlides: () -> Unit,
    integrityAlert: Boolean? = null,
    onIntegrity: () -> Unit = {},
    contentCount: Int = 0,
    onContent: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FrameFilter.entries.filter { it != FrameFilter.Tagged }.forEach { f -> FilterChip(filterLabel(f), f == current) { onSelect(f) } }
        if (current != FrameFilter.Deleted) {
            FilterChip(if (contentCount > 0) QualityStrings.contentCount(contentCount) else QualityStrings.content(), current == FrameFilter.Tagged) { onContent() }
            FilterChip(QualityStrings.sharpest(), false) { onSharpest() }
            FilterChip(QualityStrings.duplicates(), false) { onDuplicates() }
            FilterChip(QualityStrings.slidesPdf(), false) { onSlides() }
            if (integrityAlert != null) FilterChip(if (integrityAlert) QualityStrings.integrityAlert() else QualityStrings.integrity(), false) { onIntegrity() }
        }
    }
}

private fun filterLabel(f: FrameFilter): String = when (f) {
    FrameFilter.All -> FrameStrings.all()
    FrameFilter.Favorites -> FrameStrings.favorites()
    FrameFilter.Deleted -> FrameStrings.deleted()
    FrameFilter.Albums -> FrameStrings.items()
    FrameFilter.Tagged -> QualityStrings.content()
    FrameFilter.Sharpest -> QualityStrings.sharpest()
}

@Composable
internal fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .frameControl(FrameShapes.pill, selected = selected, glowAlpha = if (selected) 0.8f else 0f)
            .clip(FrameShapes.pill)
            .frameSelectable(selected, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        FrameText(
            label,
            style = FrameTheme.typography.label,
            color = if (selected) colors.primary else colors.onSurfaceVariant
        )
    }
}
