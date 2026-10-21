package com.frameout.app.frames

import com.frameout.designsystem.theme.rememberFrameAdaptive
import com.frameout.designsystem.theme.frameControl
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.app.thumb.FocusedImage
import com.frameout.app.thumb.ThumbPlaceholder
import com.frameout.app.thumb.rememberThumb
import com.frameout.core.db.AlbumCover
import com.frameout.core.quality.TagNames
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.core.util.ThumbCache
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private const val FOCUS_THUMB = 512

@Composable
internal fun FramesAlbums(
    state: FramesState,
    cover: suspend (String) -> AlbumCover?,
    onOpen: (String) -> Unit,
    onRescan: () -> Unit,
    clearance: Dp,
    columns: Int,
    onColumns: (Int) -> Unit
) {
    val adaptive = rememberFrameAdaptive()
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    val grouped = remember(state.tagCounts) { state.tagCounts.groupBy { TagNames.group(it.tag) } }
    if (state.tagCounts.isEmpty()) {
        AlbumsEmpty(state, onRescan)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(adaptive.columns(columns)),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp + adaptive.railInset, bottom = clearance),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().pinchColumns(columns, onColumns, 1..4)
    ) {
        if (state.scanning) item(key = "scan", span = { GridItemSpan(maxLineSpan) }) { ScanBanner(state.scanDone, state.scanTotal, state.scanFailed) }
        ALBUM_GROUP_ORDER.forEach { group ->
            val albums = grouped[group].orEmpty()
            if (albums.isNotEmpty()) {
                item(key = "h:$group", span = { GridItemSpan(maxLineSpan) }) { GroupTitle(TagNames.groupTitle(group, arabic)) }
                items(albums, key = { it.tag }, contentType = { "album" }) { t ->
                    AlbumCard(TagNames.display(t.tag, arabic), t.frames, t.tag, cover) { onOpen(t.tag) }
                }
            }
        }
        if (!state.scanning) item(key = "rescan", span = { GridItemSpan(maxLineSpan) }) { RescanLink(onRescan) }
    }
}

@Composable
private fun GroupTitle(text: String) {
    FrameText(text, style = FrameTheme.typography.titleSmall, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
}

@Composable
private fun RescanLink(onRescan: () -> Unit) {
    FrameText(QualityStrings.contentRescan(), color = FrameTheme.colors.primary, modifier = Modifier.frameClickable(onClick = onRescan).padding(vertical = 14.dp))
}

@Composable
private fun ScanBanner(done: Int, total: Int, failed: Int) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(FrameShapes.pill).background(colors.primary.copy(alpha = 0.12f)).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(6.dp).background(colors.primary, FrameShapes.pill))
        FrameText(QualityStrings.contentScanProgress(done, total, failed), style = FrameTheme.typography.label, color = colors.primary, maxLines = 1)
    }
}

@Composable
private fun AlbumsEmpty(state: FramesState, onRescan: () -> Unit) {
    val colors = FrameTheme.colors
    Box(Modifier.fillMaxSize().padding(bottom = 120.dp, start = 32.dp, end = 32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(FrameIconsExtra.Album), null, Modifier.size(40.dp), colorFilter = ColorFilter.tint(colors.primary))
            FrameText(
                if (state.scanning) QualityStrings.contentScanProgress(state.scanDone, state.scanTotal, state.scanFailed) else QualityStrings.contentEmpty(),
                style = FrameTheme.typography.title
            )
            if (!state.scanning) RescanLink(onRescan)
        }
    }
}

@Composable
private fun AlbumCard(label: String, count: Int, tag: String, cover: suspend (String) -> AlbumCover?, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val albumCover = produceState<AlbumCover?>(null, tag) { value = cover(tag) }.value
    val frame = albumCover?.frame
    val focus = albumCover?.focus
    val path = frame?.let { it.thumbnailPath ?: it.fullPath }
    val size = if (focus != null) FOCUS_THUMB else ThumbCache.DEFAULT_SIZE
    val bitmap = if (path == null) null else rememberThumb("$path#$size", { ThumbCache.peek(path, size) }, { ThumbCache.load(path, size) }).value
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(FrameShapes.md)
            .background(colors.container)
            .border(1.dp, frameHairline(), FrameShapes.md)
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = "$label, ${FrameStrings.frameCount(count)}" }
    ) {
        if (bitmap != null && !bitmap.isRecycled) {
            if (focus != null) FocusedImage(bitmap, focus, Modifier.fillMaxSize())
            else Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize(), Alignment.Center) { ThumbPlaceholder() }
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 10.dp)
        ) {
            FrameText(label, style = FrameTheme.typography.label, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            FrameText(FrameStrings.frameCount(count), style = FrameTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}

@Composable
internal fun FramesAlbumBar(state: FramesState, onBack: () -> Unit, onRemove: (String) -> Unit, onRefine: () -> Unit) {
    val colors = FrameTheme.colors
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        HeaderButton(FrameIcons.Back, FrameStrings.back(), onBack)
        LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(state.tagFilter.toList(), key = { it }) { tag ->
                ActiveTagChip(TagNames.display(tag, arabic)) { onRemove(tag) }
            }
        }
        Row(
            Modifier
                .height(36.dp)
                .frameControl(FrameShapes.pill, selected = true, glowAlpha = 0.6f)
                .clip(FrameShapes.pill)
                .frameClickable(onClick = onRefine)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(painterResource(FrameIconsExtra.Plus), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(colors.primary))
            FrameText(FrameStrings.refine(), style = FrameTheme.typography.label, color = colors.primary, maxLines = 1)
        }
    }
}

@Composable
private fun ActiveTagChip(label: String, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .frameControl(FrameShapes.pill, selected = true, glowAlpha = 0.5f)
            .clip(FrameShapes.pill)
            .frameClickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FrameText(label, style = FrameTheme.typography.label, color = colors.primary, maxLines = 1)
        Image(painterResource(FrameIcons.Close), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.primary))
    }
}
