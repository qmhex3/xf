package com.frameout.app.frames

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.frameout.core.util.ScrubStrings
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.HapticOnChange
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.motion.frameFadeIn
import com.frameout.designsystem.motion.fxBubbleEnter
import com.frameout.designsystem.motion.fxBubbleExit
import com.frameout.designsystem.motion.frameFadeOut
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val LINGER_MS = 1200L
private const val MIN_SCREENS = 3

@Composable
fun FramesScrubber(grid: LazyGridState, total: Int, timeMsAt: suspend (Int) -> Long?, onScrubbing: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val currentTotal = rememberUpdatedState(total)
    val scrub = remember(grid) { ScrubberState(grid) { currentTotal.value } }
    val shown = rememberScrubberVisibility(scrub, grid)
    ScrubberEffects(scrub, timeMsAt, onScrubbing)
    val enough by remember { derivedStateOf { currentTotal.value >= scrub.visibleCount * MIN_SCREENS } }
    if (!enough) return
    var trackPx by remember { mutableIntStateOf(0) }
    val thumbPx = with(LocalDensity.current) { 56.dp.toPx() }
    Box(modifier.fillMaxHeight().onSizeChanged { trackPx = it.height }, Alignment.TopEnd) {
        AnimatedVisibility(shown || scrub.dragging, Modifier.offset { IntOffset(0, (scrub.fraction * (trackPx - thumbPx)).roundToInt()) }, frameFadeIn(), frameFadeOut()) {
            ScrubberRow(scrub, total, trackPx - thumbPx)
        }
    }
}

@Composable
private fun ScrubberRow(scrub: ScrubberState, total: Int, rangePx: Float) {
    val haptics = rememberFrameHaptics()
    val scope = rememberCoroutineScope()
    val range by rememberUpdatedState(rangePx)
    HapticOnChange(scrub.targetIndex, scrub.dragging)
    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(scrub.dragging, enter = fxBubbleEnter(), exit = fxBubbleExit()) {
            ScrubberBubble(scrub.targetIndex, total, scrub.targetMs)
        }
        Box(
            Modifier.pointerInput(Unit) {
                try {
                    detectVerticalDragGestures(
                        onDragStart = { scrub.start(); haptics.perform(FrameHaptic.DragStart) },
                        onDragEnd = { scrub.stop(); haptics.perform(FrameHaptic.DragEnd) },
                        onDragCancel = scrub::stop
                    ) { change, delta -> change.consume(); scrub.drag(delta, range) }
                } finally {
                    scrub.stop()
                }
            }.semantics {
                contentDescription = ScrubStrings.fastScroll()
                stateDescription = ScrubStrings.scrubPosition(scrub.position + 1, total)
                progressBarRangeInfo = ProgressBarRangeInfo(scrub.fraction, 0f..1f)
                setProgress { value -> scope.launch { scrub.jumpTo(value) }; true }
            }
        ) { ScrubberThumb(scrub.dragging) }
    }
}

@Composable
private fun rememberScrubberVisibility(scrub: ScrubberState, grid: LazyGridState): Boolean {
    var shown by remember { mutableStateOf(false) }
    val active = scrub.dragging || grid.isScrollInProgress
    LaunchedEffect(active) {
        if (active) shown = true else { delay(LINGER_MS); shown = false }
    }
    return shown
}

@Composable
private fun ScrubberEffects(scrub: ScrubberState, timeMsAt: suspend (Int) -> Long?, onScrubbing: (Boolean) -> Unit) {
    val report by rememberUpdatedState(onScrubbing)
    DisposableEffect(Unit) { onDispose { report(false) } }
    LaunchedEffect(scrub.dragging) {
        onScrubbing(scrub.dragging)
        if (!scrub.dragging) return@LaunchedEffect
        launch { snapshotFlow { scrub.targetIndex }.collectLatest { scrub.scrollToTarget() } }
        snapshotFlow { scrub.targetIndex }.collectLatest { scrub.targetMs = timeMsAt(it) }
    }
}
