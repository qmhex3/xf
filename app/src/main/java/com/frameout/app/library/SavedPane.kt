package com.frameout.app.library

import com.frameout.designsystem.theme.rememberFrameAdaptive
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.frames.pinchColumns
import com.frameout.app.thumb.PrefetchAhead
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.compose.foundation.lazy.grid.items
import androidx.paging.LoadState
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun SavedPane(
    state: LibraryState,
    paging: LazyPagingItems<SavedListItem>,
    gridState: LazyGridState,
    bottomPadding: Dp,
    onToggle: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onResave: (Long) -> Unit,
    onColumns: (Int) -> Unit
) {
    when {
        paging.loadState.refresh is LoadState.Loading && paging.itemCount == 0 -> LibraryLoading()
        paging.loadState.refresh is LoadState.Error -> LibraryEmpty(
            icon = FrameIconsExtra.Album,
            title = FrameStrings.failed(),
            hint = "",
            actionLabel = FrameStrings.retry(),
            onAction = { paging.retry() }
        )
        paging.itemCount == 0 && (state.query.isNotBlank() || state.selectedAlbum != null || state.savedKind != null) -> LibraryEmpty(
            icon = FrameIcons.Search,
            title = FrameStrings.noResults(),
            hint = FrameStrings.noResultsHint()
        )
        paging.itemCount == 0 -> LibraryEmpty(
            icon = FrameIconsExtra.Album,
            title = FrameStrings.noSaved(),
            hint = FrameStrings.emptySavedHint()
        )
        else -> SavedGrid(paging, state, gridState, bottomPadding, onToggle, onOpen, onResave, onColumns)
    }
}

@Composable
private fun SavedGrid(
    paging: LazyPagingItems<SavedListItem>,
    state: LibraryState,
    gridState: LazyGridState,
    bottomPadding: Dp,
    onToggle: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onResave: (Long) -> Unit,
    onColumns: (Int) -> Unit
) {
    val appName = FrameStrings.appName()
    val counts = remember(state.albums) { state.albums.associate { it.album.ifBlank { appName } to it.total } }
    val showCounts = state.query.isBlank()
    val context = LocalContext.current
    val adaptive = rememberFrameAdaptive()
    PrefetchAhead(gridState, adaptive.columns(state.columns) * 4) { index ->
        if (index < paging.itemCount) (paging.peek(index) as? SavedListItem.Image)?.let { SavedThumbs.load(context, it.item.uri) }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(adaptive.columns(state.columns)),
        contentPadding = PaddingValues(top = 4.dp, bottom = bottomPadding, start = 14.dp, end = 14.dp + adaptive.railInset),
        modifier = Modifier.fillMaxSize().pinchColumns(state.columns, onColumns, 2..4)
    ) {
        items(
            count = paging.itemCount,
            key = { index ->
                when (val row = paging.peek(index)) {
                    is SavedListItem.Image -> "i${row.item.id}"
                    is SavedListItem.Header -> "h$index"
                    null -> "p$index"
                }
            },
            contentType = { index -> if (paging.peek(index) is SavedListItem.Header) 0 else 1 },
            span = { index -> if (paging.peek(index) is SavedListItem.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) }
        ) { index ->
            when (val row = paging[index]) {
                is SavedListItem.Header -> Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FrameText(row.title, style = FrameTheme.typography.titleSmall, color = FrameTheme.colors.onSurface)
                    if (showCounts) counts[row.title]?.let { FrameText(it.toString(), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant) }
                }
                is SavedListItem.Image -> Box {
                    SavedImageCard(
                        item = row.item,
                        selected = row.item.id in state.selectedSaved,
                        onClick = { if (state.selectionMode) onToggle(row.item.id) else onOpen(row.item.id) },
                        onLongClick = { onToggle(row.item.id) },
                        onResave = { onResave(row.item.id) },
                        selectionMode = state.selectionMode
                    )
                }
                null -> Unit
            }
        }
    }
}
