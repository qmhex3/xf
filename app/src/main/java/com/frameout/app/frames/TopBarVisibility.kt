package com.frameout.app.frames

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val TOP_SLACK_PX = 8
private const val SETTLE_MS = 650L

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

@Composable
fun rememberTopBarVisible(grid: LazyGridState, pinned: Boolean, settled: Boolean): Boolean {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(grid) {
        var lastIndex = grid.firstVisibleItemIndex
        var lastOffset = grid.firstVisibleItemScrollOffset
        snapshotFlow { grid.firstVisibleItemIndex to grid.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            val forward = index > lastIndex || (index == lastIndex && offset > lastOffset)
            visible = (index == 0 && offset < TOP_SLACK_PX) || !forward
            lastIndex = index
            lastOffset = offset
        }
    }
    return visible || pinned || settled
}
