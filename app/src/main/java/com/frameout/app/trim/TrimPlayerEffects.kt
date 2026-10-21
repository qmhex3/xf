package com.frameout.app.trim

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.frameout.core.prefs.AppPrefs
import com.frameout.engine.player.PlayerController
import com.frameout.engine.player.PlayerUiState
import com.frameout.engine.timeline.TrimRange
import kotlinx.coroutines.delay

@Composable
internal fun TrimPlayerEffects(uri: Uri, source: Uri?, controller: PlayerController, playerState: PlayerUiState, prefs: AppPrefs) {
    var restored by remember(uri) { mutableStateOf(false) }
    LaunchedEffect(uri, source) {
        if (source != null) controller.setUri(source)
        if (prefs.lastVideoUri() != uri.toString()) prefs.setLastVideo(uri.toString(), 0L)
    }
    LaunchedEffect(playerState.durationMs) {
        if (playerState.durationMs > 0 && !restored) {
            restored = true
            if (prefs.lastVideoUri() == uri.toString()) controller.seekTo(prefs.lastVideoPosition())
        }
    }
    LaunchedEffect(controller) {
        while (true) {
            controller.poll()
            if (controller.playing) withFrameNanos { } else delay(IDLE_POLL_MS)
        }
    }
    val owner = LocalActivity.current as? LifecycleOwner
    DisposableEffect(owner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) controller.pause()
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    DisposableEffect(Unit) {
        onDispose {
            prefs.setLastVideo(uri.toString(), controller.positionNow())
            controller.release()
        }
    }
}

@Composable
internal fun TrimWindowEffect(controller: PlayerController, range: TrimRange, durationMs: Long) {
    LaunchedEffect(controller, range, durationMs) {
        val bounded = range.clamped(durationMs)
        if (range.useFull || durationMs <= 0L) { controller.clearWindow(); return@LaunchedEffect }
        controller.setWindow(bounded.startMs, bounded.endMs)
        delay(REPAIR_DELAY_MS)
        controller.repairPlayhead()
    }
    DisposableEffect(controller) {
        onDispose { controller.clearWindow() }
    }
}

private const val IDLE_POLL_MS = 200L
private const val REPAIR_DELAY_MS = 180L
