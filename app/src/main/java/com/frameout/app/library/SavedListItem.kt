package com.frameout.app.library

import com.frameout.core.db.SavedImageEntity

sealed interface SavedListItem {
    data class Header(val title: String) : SavedListItem
    data class Image(val item: SavedImageEntity) : SavedListItem
}
