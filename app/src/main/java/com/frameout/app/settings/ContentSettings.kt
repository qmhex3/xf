package com.frameout.app.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import com.frameout.app.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frameout.core.quality.content.ContentCatalog
import com.frameout.core.quality.content.ContentGroup
import com.frameout.core.quality.content.ContentSpec
import com.frameout.core.quality.content.ContentTag
import com.frameout.core.util.ContentStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private const val MATCH_ANY = "any"
private const val MATCH_ALL = "all"

@Composable
internal fun ContentSettings(spec: ContentSpec, onChange: (ContentSpec) -> Unit) {
    SettingsDivider(0.dp)
    ContentTagPicker(spec, onChange, Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
}

@Composable
internal fun ContentTagPicker(spec: ContentSpec, onChange: (ContentSpec) -> Unit, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val arabic = FrameStrings.isAr()
    val groups = ContentGroup.values().filter { ContentCatalog.inGroup(it).isNotEmpty() }
    var open by rememberSaveable { mutableStateOf(groups.firstOrNull { g -> ContentCatalog.inGroup(g).any { it.id in spec.tags } }?.id ?: groups.first().id) }
    Column(modifier, Arrangement.spacedBy(10.dp)) {
        PickerHeader(spec.tags.size) { onChange(spec.copy(tags = emptySet())) }
        if (spec.tags.isEmpty()) {
            FrameText(ContentStrings.pickHint(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        groups.forEach { group ->
            GroupCard(group, group.title(arabic), spec.tags, open == group.id, { open = if (open == group.id) "" else group.id }) { ids, on ->
                onChange(spec.copy(tags = if (on) spec.tags + ids else spec.tags - ids.toSet()))
            }
        }
        Spacer(Modifier.height(4.dp))
        ContentOptions(spec, onChange)
    }
}

@Composable
private fun ContentOptions(spec: ContentSpec, onChange: (ContentSpec) -> Unit) {
    val colors = FrameTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OptionBlock(ContentStrings.matchLabel()) {
            SettingsSegmented(
                listOf(SegmentOption(MATCH_ANY, ContentStrings.matchAny()), SegmentOption(MATCH_ALL, ContentStrings.matchAll())),
                if (spec.all) MATCH_ALL else MATCH_ANY
            ) { onChange(spec.copy(all = it == MATCH_ALL)) }
        }
        OptionBlock(ContentStrings.strictLabel()) {
            val levels = listOf(ContentSpec.STRICT_LOOSE, ContentSpec.STRICT_BALANCED, ContentSpec.STRICT_STRICT)
            SettingsSegmented(levels.map { SegmentOption(it.toString(), ContentStrings.strictName(it)) }, spec.strict.toString()) {
                onChange(spec.copy(strict = it.toInt()))
            }
            FrameText(ContentStrings.strictHelp(spec.strict), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        OptionBlock(ContentStrings.intervalLabel()) {
            SettingsSegmented(ContentSpec.INTERVALS_MS.map { SegmentOption(it.toString(), ContentStrings.seconds(it)) }, spec.millis.toString()) {
                onChange(spec.copy(millis = it.toLong()))
            }
        }
        OptionBlock(ContentStrings.gapLabel()) {
            val gaps = ContentSpec.GAPS_MS.map { ms -> SegmentOption(ms.toString(), if (ms == 0L) ContentStrings.gapNone() else ContentStrings.seconds(ms)) }
            SettingsSegmented(gaps, spec.gapMs.toString()) { onChange(spec.copy(gapMs = it.toLong())) }
        }
        FrameText(ContentStrings.slow(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun PickerHeader(count: Int, onClear: () -> Unit) {
    val colors = FrameTheme.colors
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
        FrameText(ContentStrings.title(), style = rowTitleStyle(), modifier = Modifier.weight(1f))
        if (count > 0) {
            FrameText(ContentStrings.selected(count), style = FrameTheme.typography.label, color = colors.primary)
            Box(
                Modifier.clip(FrameShapes.pill).frameClickable(onClick = onClear).padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                FrameText(ContentStrings.clear(), style = FrameTheme.typography.label, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OptionBlock(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FrameText(label, style = FrameTheme.typography.label, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        content()
    }
}

@Composable
private fun GroupCard(
    group: ContentGroup, title: String, selected: Set<String>, expanded: Boolean,
    onToggleOpen: () -> Unit, onSet: (List<String>, Boolean) -> Unit
) {
    val colors = FrameTheme.colors
    val tags = ContentCatalog.inGroup(group)
    val count = tags.count { it.id in selected }
    val edge by animateColorAsState(if (count > 0) colors.primary.copy(alpha = 0.5f) else frameHairline(), frameSpring(), label = "groupEdge")
    Column(Modifier.fillMaxWidth().clip(FrameShapes.lg).background(colors.onSurface.copy(alpha = 0.04f)).border(1.dp, edge, FrameShapes.lg)) {
        GroupHeader(group.id, title, count, tags.size, expanded, onToggleOpen)
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), Arrangement.spacedBy(12.dp)) {
                ChipFlow(tags, selected) { onSet(listOf(it), it !in selected) }
                GroupAction(count == tags.size) { onSet(tags.map { t -> t.id }, count != tags.size) }
            }
        }
    }
}

@Composable
private fun GroupHeader(id: String, title: String, count: Int, total: Int, expanded: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val turn by animateFloatAsState(if (expanded) 180f else 0f, frameSpring(), label = "groupTurn")
    Row(
        Modifier.fillMaxWidth().frameSelectable(expanded, Role.Button, onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        Arrangement.spacedBy(12.dp), Alignment.CenterVertically
    ) {
        SettingsIconTile(contentGroupIcon(id), count > 0)
        FrameText(title, style = rowTitleStyle(), modifier = Modifier.weight(1f))
        FrameText(
            if (count > 0) "$count / $total" else "$total",
            style = FrameTheme.typography.label, color = if (count > 0) colors.primary else colors.onSurfaceVariant
        )
        Image(painterResource(R.drawable.ic_cgroup_chevron), null, Modifier.size(20.dp).rotate(turn), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(tags: List<ContentTag>, selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag -> TagChip(tag, tag.id in selected) { onToggle(tag.id) } }
    }
}

@Composable
private fun GroupAction(allOn: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(FrameShapes.pill).frameClickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 6.dp)) {
        FrameText(
            if (allOn) ContentStrings.groupNone() else ContentStrings.groupAll(),
            style = FrameTheme.typography.label, color = FrameTheme.colors.primary
        )
    }
}

@Composable
private fun TagChip(tag: ContentTag, selected: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val fill by animateColorAsState(if (selected) colors.primary.copy(alpha = 0.16f) else colors.onSurface.copy(alpha = 0.06f), frameSpring(), label = "chipFill")
    val edge by animateColorAsState(if (selected) colors.primary else frameHairline(), frameSpring(), label = "chipEdge")
    val ink by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, frameSpring(), label = "chipInk")
    Row(
        Modifier
            .height(40.dp)
            .clip(FrameShapes.pill)
            .background(fill)
            .border(if (selected) 1.5.dp else 1.dp, edge, FrameShapes.pill)
            .frameSelectable(selected, Role.Checkbox, onClick)
            .padding(horizontal = 12.dp),
        Arrangement.spacedBy(8.dp), Alignment.CenterVertically
    ) {
        Image(painterResource(contentTagIcon(tag.id)), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(ink))
        FrameText(
            tag.title(FrameStrings.isAr()),
            style = FrameTheme.typography.body.copy(fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal),
            color = if (selected) colors.primary else colors.onSurface,
            maxLines = 1
        )
    }
}
