package com.frameout.app.picker

import com.frameout.core.media.MediaAlbum
import com.frameout.core.media.MediaFilterCount
import com.frameout.core.media.MediaItem
import com.frameout.core.media.MediaKind

data class PickerState(
    val albums: List<MediaAlbum> = emptyList(),
    val filters: List<MediaFilterCount> = emptyList(),
    val selectedKind: MediaKind? = null,
    val selectedBucket: String? = null,
    val selected: LinkedHashMap<String, MediaItem> = linkedMapOf(),
    val loading: Boolean = true,
    val permissionGranted: Boolean = false,
    val error: String? = null,
    val generation: Int = 0
)

fun PickerState.toggleSelect(item: MediaItem): PickerState {
    val map = LinkedHashMap(selected)
    if (map.containsKey(item.uri)) map.remove(item.uri) else map[item.uri] = item
    return copy(selected = map)
}

fun PickerState.moveSelect(from: Int, to: Int): PickerState {
    val entries = selected.values.toMutableList()
    if (from !in entries.indices || to !in entries.indices || from == to) return this
    entries.add(to, entries.removeAt(from))
    return copy(selected = entries.toSelectionMap())
}

fun PickerState.restoreSelect(items: List<MediaItem>): PickerState = copy(selected = items.toSelectionMap())

fun PickerState.restoreAt(item: MediaItem, index: Int): PickerState {
    if (selected.containsKey(item.uri)) return this
    val entries = selected.values.toMutableList()
    entries.add(index.coerceIn(0, entries.size), item)
    return copy(selected = entries.toSelectionMap())
}

private fun List<MediaItem>.toSelectionMap(): LinkedHashMap<String, MediaItem> {
    val map = LinkedHashMap<String, MediaItem>()
    forEach { map[it.uri] = it }
    return map
}
