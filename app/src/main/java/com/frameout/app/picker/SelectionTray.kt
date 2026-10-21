package com.frameout.app.picker

import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.designsystem.glass.frameGlow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.frameout.core.media.MediaItem
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun SelectionTray(
    selected: List<MediaItem>,
    onRemove: (MediaItem) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onMove: (from: Int, to: Int) -> Unit = { _, _ -> }
) {
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val shown = remember { mutableStateOf(selected) }
    if (selected.isNotEmpty()) shown.value = selected
    val enter = if (reduceMotion) {
        EnterTransition.None
    } else {
        slideInVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)) { it } +
            fadeIn(tween(160))
    }
    val exit = if (reduceMotion) {
        ExitTransition.None
    } else {
        slideOutVertically(tween(240)) { it } + fadeOut(tween(180)) + scaleOut(tween(240), targetScale = 0.94f)
    }
    AnimatedVisibility(selected.isNotEmpty(), modifier, enter = enter, exit = exit) {
        TrayCard(shown.value, onRemove, onContinue, onMove)
    }
}

@Composable
private fun rememberArrivalUri(selected: List<MediaItem>, listState: LazyListState): String? {
    var previousSize by remember { mutableStateOf(selected.size) }
    var arrived by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selected.size) {
        if (selected.size > previousSize) {
            arrived = selected.last().uri
            listState.animateScrollToItem(selected.lastIndex)
        }
        previousSize = selected.size
    }
    return arrived
}

@Composable
private fun TrayCard(
    selected: List<MediaItem>,
    onRemove: (MediaItem) -> Unit,
    onContinue: () -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val flight = LocalPickerFlight.current
    val haptics = rememberFrameHaptics()
    val listState = rememberLazyListState()
    val drag = remember { TrayDrag() }
    val arrivedUri = rememberArrivalUri(selected, listState)
    val latestMove by rememberUpdatedState(onMove)
    DisposableEffect(Unit) {
        onDispose {
            flight?.trayCard = null
            flight?.updateTrayRow(null)
        }
    }
    val dragStep: (String, Float) -> Unit = { uri, delta ->
        if (drag.step(uri, delta, listState) { from, to -> latestMove(from, to) }) haptics.perform(FrameHaptic.Select)
    }
    Column(Modifier.trayCardSurface(flight)) {
        TrayHeader(selected.size, showHint = selected.size > 1 && !drag.everDragged)
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            LazyRow(
                state = listState,
                modifier = Modifier.weight(1f).onGloballyPositioned { flight?.updateTrayRow(it.boundsInWindow()) },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selected, key = { it.uri }) { item ->
                    TrayThumb(item, item.uri == arrivedUri, drag, dragStep, { onRemove(item) })
                }
            }
            Spacer(Modifier.width(10.dp))
            ContinueButton(selected.size, onContinue)
        }
    }
}

@Composable
private fun Modifier.trayCardSurface(flight: PickerFlight?): Modifier {
    val colors = FrameTheme.colors
    val shape = FrameShapes.lg
    val solid = LocalSystemA11y.current.reduceTransparency
    val base = if (colors.isDark) androidx.compose.ui.graphics.lerp(colors.surface, Color.Black, 0.35f) else colors.surface
    val top = base.copy(alpha = if (solid) 1f else 0.80f)
    val bottom = base.copy(alpha = if (solid) 1f else 0.88f)
    val rim = if (solid) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.04f))) else frameCardEdge(colors.primary, 0.55f)
    return this
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 14.dp, vertical = 12.dp)
        .onGloballyPositioned { flight?.trayCard = it.boundsInWindow() }
        .frameGlow(colors.primary, { if (colors.isDark) 0.22f else 0.12f }, 16.dp, { 0.12f })
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(1.dp, rim, shape)
        .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 10.dp)
}
