package com.frameout.app.frames

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

@Stable
class ScrubberState(private val grid: LazyGridState, private val total: () -> Int) {
    var dragging by mutableStateOf(false)
        private set
    var targetIndex by mutableIntStateOf(0)
        private set
    var targetMs by mutableStateOf<Long?>(null)
    private var dragFraction by mutableFloatStateOf(0f)

    val visibleCount: Int get() = grid.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)

    private val maxFirst: Int get() = (total() - visibleCount).coerceAtLeast(0)

    val fraction: Float
        get() = if (dragging) dragFraction else restingFraction()

    val position: Int get() = if (dragging) targetIndex else grid.firstVisibleItemIndex

    private fun restingFraction(): Float =
        if (maxFirst == 0) 0f else (grid.firstVisibleItemIndex.toFloat() / maxFirst).coerceIn(0f, 1f)

    fun start() {
        dragFraction = restingFraction()
        targetIndex = grid.firstVisibleItemIndex
        dragging = true
    }

    fun drag(deltaPx: Float, rangePx: Float) {
        if (rangePx <= 0f) return
        dragFraction = (dragFraction + deltaPx / rangePx).coerceIn(0f, 1f)
        targetIndex = (dragFraction * maxFirst).roundToInt()
    }

    fun stop() {
        dragging = false
    }

    suspend fun jumpTo(fraction: Float) {
        grid.scrollToItem((fraction.coerceIn(0f, 1f) * maxFirst).roundToInt())
    }

    suspend fun scrollToTarget() {
        grid.scrollToItem(targetIndex)
    }
}
