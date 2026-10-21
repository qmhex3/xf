package com.frameout.app.picker

import com.frameout.designsystem.theme.frameControl
import androidx.compose.foundation.Image
import com.frameout.designsystem.glass.framePopupSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.frameout.core.media.MediaAlbum
import com.frameout.core.media.MediaFilterCount
import com.frameout.core.media.MediaKind
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline

private val ControlHeight = 36.dp

@Composable
fun PickerFilterBar(
    albums: List<MediaAlbum>,
    selectedBucket: String?,
    onAlbum: (String?) -> Unit,
    filters: List<MediaFilterCount>,
    selectedKind: MediaKind?,
    onKind: (MediaKind?) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).fadingEdges(14.dp)) { AlbumChips(albums, selectedBucket, onAlbum) }
        if (filters.isNotEmpty()) {
            KindFilterButton(filters, selectedKind, selectedBucket == null, onKind, Modifier.padding(start = 6.dp, end = 12.dp))
        }
    }
}

private fun Modifier.fadingEdges(width: androidx.compose.ui.unit.Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val w = width.toPx().coerceAtMost(size.width / 2f)
        val f = w / size.width
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent, f to Color.Black, 1f - f to Color.Black, 1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

@Composable
fun AlbumChips(albums: List<MediaAlbum>, selectedBucket: String?, onSelect: (String?) -> Unit) {
    if (albums.size < 2) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "__all") { AlbumChip(FrameStrings.allMedia(), selectedBucket == null) { onSelect(null) } }
        items(albums, key = { it.bucketId }) { album ->
            AlbumChip(album.name, album.bucketId == selectedBucket) { onSelect(album.bucketId) }
        }
    }
}

@Composable
private fun AlbumChip(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val fill = if (active) colors.primary.copy(alpha = if (colors.isDark) 0.20f else 0.14f) else colors.fieldFill
    val edge = if (active) colors.primary.copy(alpha = 0.40f) else frameHairline()
    Box(
        Modifier
            .frameSelectable(active, Role.RadioButton, onClick)
            .height(ControlHeight)
            .frameControl(FrameShapes.pill, selected = active, glowAlpha = if (active) 0.8f else 0f)
            .clip(FrameShapes.pill)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        FrameText(label, style = FrameTheme.typography.label, color = if (active) colors.primary else colors.onSurface)
    }
}

@Composable
private fun KindFilterButton(
    filters: List<MediaFilterCount>,
    selected: MediaKind?,
    showCounts: Boolean,
    onSelect: (MediaKind?) -> Unit,
    modifier: Modifier
) {
    val colors = FrameTheme.colors
    var open by remember { mutableStateOf(false) }
    val active = selected != null
    val tint = if (active) colors.primary else colors.onSurfaceVariant
    val description = FrameStrings.filterByType() + ": " + selected.label()
    Box(modifier) {
        Row(
            Modifier
                .frameClickable { open = !open }
                .semantics { contentDescription = description }
                .height(ControlHeight)
                .widthIn(min = ControlHeight)
                .frameControl(FrameShapes.pill, selected = active, glowAlpha = if (active) 0.8f else 0f)
                .clip(FrameShapes.pill)
                .padding(horizontal = if (active) 12.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            Image(painterResource(FrameIcons.Filter), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(tint))
            if (active) {
                Image(painterResource(selected.icon()), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(tint))
                FrameText(selected.label(), style = FrameTheme.typography.label, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 80.dp))
            }
        }
        if (open) {
            val gap = with(LocalDensity.current) { (ControlHeight + 6.dp).roundToPx() }
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, gap),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)
            ) {
                KindMenu(filters, selected, showCounts) { kind ->
                    open = false
                    onSelect(kind)
                }
            }
        }
    }
}

@Composable
private fun KindMenu(filters: List<MediaFilterCount>, selected: MediaKind?, showCounts: Boolean, onSelect: (MediaKind?) -> Unit) {
    val colors = FrameTheme.colors
    Column(
        Modifier
            .width(240.dp)
            .heightIn(max = 420.dp)
            .framePopupSurface(FrameShapes.xl)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        FrameText(
            FrameStrings.filterByType(),
            style = FrameTheme.typography.label,
            color = colors.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp)
        )
        filters.forEach { filter -> KindOption(filter, filter.kind == selected, showCounts) { onSelect(filter.kind) } }
    }
}

@Composable
private fun KindOption(filter: MediaFilterCount, active: Boolean, showCount: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint = if (active) colors.primary else colors.onSurface
    Row(
        Modifier
            .frameSelectable(active, Role.RadioButton, onClick)
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 6.dp)
            .clip(FrameShapes.sm)
            .background(if (active) colors.primary.copy(alpha = 0.14f) else Color.Transparent)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(painterResource(filter.kind.icon()), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(if (active) colors.primary else colors.onSurfaceVariant))
        FrameText(filter.kind.label(), style = FrameTheme.typography.body, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (showCount) FrameText("${filter.count}", style = FrameTheme.typography.monoSmall, color = if (active) colors.primary else colors.onSurfaceVariant)
    }
}
