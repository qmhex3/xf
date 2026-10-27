package com.frameout.app.frames

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.SettingsCard
import com.frameout.app.settings.SettingsSection
import com.frameout.core.quality.TagGroup
import com.frameout.core.quality.TagNames
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPanel(state: FramesState, onToggle: (String) -> Unit, onClose: () -> Unit) {
    val colors = FrameTheme.colors
    val lang = com.frameout.core.util.AppLang.of(LocalConfiguration.current.locales[0].toLanguageTag())
    val grouped = remember(state.refineCounts) { state.refineCounts.groupBy { TagNames.group(it.tag) } }
    FramesSheet(state.tagsOpen && state.filter == FrameFilter.Tagged, FrameStrings.refineTitle(), onClose) {
        if (state.refineCounts.isEmpty()) {
            FrameText(FrameStrings.refineNone(), color = colors.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
        }
        ALBUM_GROUP_ORDER.forEach { group ->
            val items = grouped[group].orEmpty()
            if (items.isNotEmpty()) {
                SettingsSection(TagNames.groupTitle(group, lang)) {
                    SettingsCard {
                        FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items.forEach { TagChip(TagNames.display(it.tag, lang), it.frames, it.tag in state.tagFilter) { onToggle(it.tag) } }
                        }
                    }
                }
            }
        }
    }
}

internal val ALBUM_GROUP_ORDER = listOf(TagGroup.Things, TagGroup.Categories, TagGroup.People, TagGroup.Text, TagGroup.Codes)

@Composable
internal fun TagChip(label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
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
        if (count != null) FrameText(count.toString(), style = FrameTheme.typography.monoSmall, color = if (selected) colors.primary else colors.onSurfaceVariant, maxLines = 1)
    }
}
