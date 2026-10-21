package com.frameout.app.trim

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.app.FrameOutApp
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.FrameHaptics
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameControl
import com.frameout.engine.player.PlaybackSpeeds
import com.frameout.engine.player.PlayerController
import kotlinx.coroutines.delay

internal enum class SpeedSide(val speed: Float, val arrow: String) {
    Slow(0.5f, "\u00AB"),
    Fast(2f, "\u00BB")
}

private const val EDGE_SHARE = 0.3f
private const val HOLD_DELAY_MS = 200L
private const val HINT_VISIBLE_MS = 4500L
private const val GLOW_ALPHA = 0.22f
private const val HINT_ALPHA = 0.75f

@Stable
internal class SpeedZoneState(private val prefs: AppPrefs) {
    var holding by mutableStateOf<SpeedSide?>(null)
    var hintVisible by mutableStateOf(false)
    private var used = false

    fun hintDue(): Boolean = prefs.speedHintShows() < AppPrefs.SPEED_HINT_LIMIT

    fun hintShown() = prefs.markSpeedHintShown()

    fun markUsed() {
        hintVisible = false
        if (used) return
        used = true
        prefs.markSpeedZoneUsed()
    }
}

@Composable
internal fun rememberSpeedZoneState(): SpeedZoneState {
    val prefs = (LocalContext.current.applicationContext as FrameOutApp).container.prefs
    val state = remember(prefs) { SpeedZoneState(prefs) }
    LaunchedEffect(state) {
        if (!state.hintDue()) return@LaunchedEffect
        state.hintShown()
        state.hintVisible = true
        delay(HINT_VISIBLE_MS)
        state.hintVisible = false
    }
    return state
}

@Composable
internal fun Modifier.speedHold(controller: PlayerController, state: SpeedZoneState): Modifier {
    val haptics = rememberFrameHaptics()
    return pointerInput(controller, state) {
        awaitEachGesture { holdGesture(controller, state, haptics) }
    }
}

private suspend fun AwaitPointerEventScope.holdGesture(controller: PlayerController, state: SpeedZoneState, haptics: FrameHaptics) {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    val side = when {
        down.position.x < size.width * EDGE_SHARE -> SpeedSide.Slow
        down.position.x > size.width * (1f - EDGE_SHARE) -> SpeedSide.Fast
        else -> return
    }
    val slop = viewConfiguration.touchSlop
    val aborted = withTimeoutOrNull(HOLD_DELAY_MS) {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
            if (event.changes.size > 1 || change.isConsumed || change.changedToUpIgnoreConsumed()) return@withTimeoutOrNull true
            if ((change.position - down.position).getDistance() > slop) return@withTimeoutOrNull true
        }
    }
    if (aborted != null) return
    val wasPlaying = controller.playing
    state.markUsed()
    state.holding = side
    haptics.perform(FrameHaptic.LongPress)
    controller.holdSpeed(side.speed)
    if (!wasPlaying) controller.play()
    try {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.forEach { it.consume() }
            if (event.changes.none { it.pressed }) break
        }
    } finally {
        state.holding = null
        controller.releaseSpeed()
        if (!wasPlaying) controller.pause()
    }
}

@Composable
internal fun BoxScope.SpeedZoneVisuals(state: SpeedZoneState) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.matchParentSize()) {
            SpeedEdge(SpeedSide.Slow, state, Modifier.align(Alignment.CenterStart))
            SpeedEdge(SpeedSide.Fast, state, Modifier.align(Alignment.CenterEnd))
            AnimatedVisibility(state.hintVisible, Modifier.align(Alignment.TopCenter).padding(top = 12.dp), enter = fadeIn(), exit = fadeOut()) {
                FrameText(
                    TrimStrings.speedHint(),
                    style = FrameTheme.typography.label,
                    color = FrameTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp).frameControl(FrameShapes.pill, selected = true, glowAlpha = 0.4f).padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SpeedEdge(side: SpeedSide, state: SpeedZoneState, modifier: Modifier) {
    val colors = FrameTheme.colors
    val active = state.holding == side
    val label = PlaybackSpeeds.label(side.speed)
    val glow by animateFloatAsState(if (active) 1f else 0f, label = "speedEdgeGlow")
    val hintAlpha by animateFloatAsState(if (state.hintVisible && state.holding == null) HINT_ALPHA else 0f, label = "speedEdgeHint")
    Box(
        modifier
            .fillMaxWidth(EDGE_SHARE)
            .fillMaxHeight()
            .drawBehind {
                if (glow > 0f) {
                    val tint = colors.primary.copy(alpha = GLOW_ALPHA * glow)
                    val stops = if (side == SpeedSide.Fast) listOf(Color.Transparent, tint) else listOf(tint, Color.Transparent)
                    drawRect(Brush.horizontalGradient(stops))
                }
            },
        Alignment.Center
    ) {
        if (hintAlpha > 0f) {
            FrameText(
                "${side.arrow}${side.arrow}\n$label",
                style = FrameTheme.typography.label,
                color = colors.onSurface.copy(alpha = hintAlpha),
                textAlign = TextAlign.Center
            )
        }
        AnimatedVisibility(active, enter = fadeIn(), exit = fadeOut()) {
            FrameText(
                "${side.arrow} $label",
                style = FrameTheme.typography.label,
                color = Color.White,
                modifier = Modifier.frameControl(FrameShapes.pill, selected = true, solid = true, glowAlpha = 0.8f).padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
