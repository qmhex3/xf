package com.frameout.app.frames

import com.frameout.designsystem.theme.rememberFrameAdaptive
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.composed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import com.frameout.core.quality.NormBox
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.frameout.app.thumb.PrefetchAhead
import com.frameout.core.util.ThumbCache
import com.frameout.designsystem.motion.fxAppear
import kotlin.math.roundToInt

@Composable
fun FramesGrid(
    projectId: Long,
    state: FramesState,
    vm: FramesViewModel,
    gridState: LazyGridState,
    columns: Int,
    onColumns: (Int) -> Unit,
    onOpenFrame: (Long) -> Unit,
    timeMsAt: suspend (Int) -> Long?,
    onScrubbing: (Boolean) -> Unit,
    clearance: Dp = 100.dp,
    highlightId: Long? = null
) {
    val pages = remember(projectId, state.filter, state.tagFilter, state.sharpestVersion) { vm.pagingFlow() }.collectAsLazyPagingItems()
    val waitIdle = remember(gridState) {
        val gate: suspend () -> Unit = { snapshotFlow { gridState.isScrollInProgress }.first { !it } }
        gate
    }
    val edits by remember(projectId) { vm.editsFlow(projectId) }.collectAsState(emptyMap())
    val selected = state.selected
    val selectedPositions = remember(selected) { selected.withIndex().associate { it.value to it.index + 1 } }
    val adaptive = rememberFrameAdaptive()
    val pinch = Modifier.pinchColumns(columns, onColumns)
    val seen = remember(projectId) { HashSet<Any>() }
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(state = gridState, columns = GridCells.Fixed(adaptive.columns(columns)), contentPadding = PaddingValues(4.dp, 4.dp, 4.dp + adaptive.railInset, clearance), modifier = Modifier.fillMaxSize().then(pinch)) {
            items(pages.itemCount, key = { i -> pages.peek(i)?.id ?: i }, contentType = { "frame" }) { i ->
                val frame = pages[i]
                if (frame == null) FramePlaceholder() else {
                    val index = selectedPositions[frame.id]
                    val tagged = state.filter == FrameFilter.Tagged
                    val focus = if (tagged) produceState<NormBox?>(null, frame.id, state.tagFilter) { value = vm.albumFocus(frame.id, state.tagFilter) }.value else null
                    FrameCell(frame, index != null, index, state.fit, { if (selected.isNotEmpty() || state.isDeletedView) vm.toggleSelect(frame.id) else onOpenFrame(frame.id) }, { vm.toggleSelect(frame.id) }, { vm.toggleFavorite(frame.id, frame.isFavorite) }, waitIdle, edits[frame.id], focus, Modifier.fxAppear(frame.id, seen, i % 8), hint = frame.id == highlightId)
                }
            }
        }
        PrefetchAhead(gridState, adaptive.columns(columns) * 4) { i ->
            if (i < pages.itemCount) pages.peek(i)?.let { ThumbCache.load(it.thumbnailPath ?: it.fullPath) }
        }
        FramesScrubber(gridState, state.frameCount, timeMsAt, onScrubbing, Modifier.align(Alignment.TopEnd).padding(end = adaptive.railInset, bottom = clearance))
    }
}

internal fun Modifier.pinchColumns(columns: Int, onColumns: (Int) -> Unit, range: IntRange = 2..8): Modifier = composed {
    val current by rememberUpdatedState(columns)
    val apply by rememberUpdatedState(onColumns)
    val haptics = com.frameout.designsystem.haptics.rememberFrameHaptics()
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var scale = 1f
            var start = current
            var pinching = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    if (!pinching) { pinching = true; scale = 1f; start = current }
                    scale *= event.calculateZoom()
                    val next = (start / scale).roundToInt().coerceIn(range.first, range.last)
                    if (next != current) {
                        haptics.perform(com.frameout.designsystem.haptics.FrameHaptic.Select)
                        apply(next)
                    }
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
