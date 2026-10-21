package com.frameout.app.picker

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class TrayDrag {
    var uri by mutableStateOf<String?>(null)
    var offset by mutableFloatStateOf(0f)
    var everDragged by mutableStateOf(false)

    fun begin(key: String) {
        uri = key
        offset = 0f
        everDragged = true
    }

    fun end() {
        uri = null
        offset = 0f
    }

    fun step(key: String, delta: Float, listState: LazyListState, onMove: (Int, Int) -> Unit): Boolean {
        offset += delta
        val items = listState.layoutInfo.visibleItemsInfo
        val current = items.firstOrNull { it.key == key } ?: return false
        val center = current.offset + current.size / 2f + offset
        val target = items.firstOrNull { it.key != key && center >= it.offset && center < it.offset + it.size } ?: return false
        onMove(current.index, target.index)
        offset -= (target.offset - current.offset)
        return true
    }
}
