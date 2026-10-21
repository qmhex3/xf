package com.frameout.app.viewer

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.FrameHaptics
import com.frameout.designsystem.haptics.rememberFrameHaptics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

internal enum class PageTransition { Depth, Parallax, Cards }

internal object ViewerTransitions {
    val style: PageTransition = PageTransition.Depth
}

private const val NAV_WAIT_MS = 900L
private const val SNAP_THRESHOLD = 0.32f

@Composable
internal fun rememberViewerPager(vm: ViewerViewModel): PagerState {
    val pager = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val haptics = rememberFrameHaptics()
    LaunchedEffect(pager, vm) {
        snapshotFlow { pager.settledPage }.collect { page -> if (page != 1) settle(pager, vm, haptics, page == 2) }
    }
    return pager
}

private suspend fun settle(pager: PagerState, vm: ViewerViewModel, haptics: FrameHaptics, forward: Boolean) {
    val now = vm.state.value
    if (!(if (forward) now.canGoNext else now.canGoPrevious)) {
        haptics.perform(FrameHaptic.Reject)
        pager.animateScrollToPage(1, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        return
    }
    haptics.perform(FrameHaptic.Select)
    val before = now.current?.id
    vm.goTo(if (forward) 1 else -1)
    withTimeoutOrNull(NAV_WAIT_MS) { vm.state.first { it.current?.id != before } }
    pager.scrollToPage(1)
}

@Composable
internal fun viewerFling(pager: PagerState) = PagerDefaults.flingBehavior(
    state = pager,
    snapAnimationSpec = spring(dampingRatio = 0.88f, stiffness = 340f),
    snapPositionalThreshold = SNAP_THRESHOLD
)

internal class HeldNeighbors {
    var previous: Bitmap? = null
    var next: Bitmap? = null
}

@Composable
internal fun rememberHeldNeighbors(): HeldNeighbors = remember { HeldNeighbors() }

internal fun HeldNeighbors.sync(pager: PagerState, state: ViewerState) {
    if (pager.settledPage == 1 && !pager.isScrollInProgress) {
        previous = state.previousBitmap
        next = state.nextBitmap
    }
}

internal fun HeldNeighbors.previousFor(pager: PagerState, state: ViewerState): Bitmap? =
    if (pager.settledPage != 1) previous ?: state.previousBitmap else state.previousBitmap

internal fun HeldNeighbors.nextFor(pager: PagerState, state: ViewerState): Bitmap? =
    if (pager.settledPage != 1) next ?: state.nextBitmap else state.nextBitmap

@Composable
internal fun PagerPage(pager: PagerState, page: Int, content: @Composable () -> Unit) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val style = ViewerTransitions.style
    Box(
        Modifier
            .fillMaxSize()
            .zIndex((1 - page).toFloat())
            .graphicsLayer { applyTransition(style, pager.pagePosition(page), size.width, rtl, density) }
    ) { content() }
}

private fun PagerState.pagePosition(page: Int): Float = page - currentPage - currentPageOffsetFraction

private fun androidx.compose.ui.graphics.GraphicsLayerScope.applyTransition(style: PageTransition, position: Float, width: Float, rtl: Boolean, density: Float) {
    val sign = if (rtl) -1f else 1f
    val distance = abs(position).coerceAtMost(1f)
    when (style) {
        PageTransition.Depth -> {
            if (position > 0f) {
                translationX = -position * width * sign
                val s = DEPTH_MIN + (1f - DEPTH_MIN) * (1f - distance)
                scaleX = s
                scaleY = s
                alpha = 1f - distance
            } else {
                alpha = 1f - distance * 0.15f
            }
        }
        PageTransition.Parallax -> {
            translationX = -position * width * PARALLAX * sign
            val s = 1f - 0.08f * distance
            scaleX = s
            scaleY = s
            alpha = if (distance >= 1f) 0f else 0.25f + 0.75f * (1f - distance)
        }
        PageTransition.Cards -> {
            cameraDistance = 14f * density
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (position * sign > 0f) 0f else 1f, 0.5f)
            rotationY = -position * CARD_TILT * sign
            val s = 1f - 0.1f * distance
            scaleX = s
            scaleY = s
            alpha = 1f - 0.5f * distance
        }
    }
}

private const val DEPTH_MIN = 0.84f
private const val PARALLAX = 0.28f
private const val CARD_TILT = 32f
