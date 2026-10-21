package com.frameout.app.library

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState

fun LazyListState.itemPosition(key: Any): Float {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return 0f
    val half = (info.viewportEndOffset - info.viewportStartOffset) / 2f
    if (half <= 0f) return 0f
    return (item.offset + item.size / 2f - (info.viewportStartOffset + half)) / half
}

fun LazyGridState.itemPosition(key: Any): Float {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return 0f
    val half = (info.viewportEndOffset - info.viewportStartOffset) / 2f
    if (half <= 0f) return 0f
    return (item.offset.y + item.size.height / 2f - (info.viewportStartOffset + half)) / half
}
