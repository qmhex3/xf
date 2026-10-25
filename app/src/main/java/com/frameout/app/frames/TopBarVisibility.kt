package com.frameout.app.frames

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val TOP_SLACK_PX = 8
private const val SETTLE_MS = 650L
private val TOP_BAR_TRAVEL = 28.dp

@Composable
fun rememberScrollSettled(grid: LazyGridState): Boolean {
    var settled by remember { mutableStateOf(true) }
    LaunchedEffect(grid) {
        snapshotFlow { grid.isScrollInProgress }.collectLatest { moving ->
            if (moving) {
                settled = false
            } else {
                delay(SETTLE_MS)
                settled = true
            }
        }
    }
    return settled
}

@Stable
class TopBarScroll internal constructor(private val grid: LazyGridState, private val thresholdPx: Float) {
    var shown by mutableStateOf(true)
        private set
    private var travel = 0f

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val dy = available.y
            if (dy == 0f) return Offset.Zero
            if (travel != 0f && (travel > 0f) != (dy > 0f)) travel = 0f
            travel += dy
            val atTop = grid.firstVisibleItemIndex == 0 && grid.firstVisibleItemScrollOffset < TOP_SLACK_PX
            when {
                atTop && dy > 0f -> shown = true
                travel <= -thresholdPx -> shown = false
                travel >= thresholdPx -> shown = true
            }
            return Offset.Zero
        }
    }

    fun visible(pinned: Boolean, settled: Boolean): Boolean {
        val atTop = grid.firstVisibleItemIndex == 0 && grid.firstVisibleItemScrollOffset < TOP_SLACK_PX
        return shown || atTop || pinned || (settled && grid.canScrollForward)
    }
}

@Composable
fun rememberTopBarScroll(grid: LazyGridState): TopBarScroll {
    val threshold = with(LocalDensity.current) { TOP_BAR_TRAVEL.toPx() }
    return remember(grid, threshold) { TopBarScroll(grid, threshold) }
}
