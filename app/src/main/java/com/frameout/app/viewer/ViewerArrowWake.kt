package com.frameout.app.viewer

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart

private const val IDLE_MS = 2200L
private const val WAKE_GUARD_MS = 320L
private const val POKE_GAP_MS = 120L
private const val SHOW_MS = 140
private const val HIDE_MS = 420

internal class ArrowWake {
    val alpha = Animatable(1f)
    var interactive by mutableStateOf(true)
        private set
    private var lastPoke = 0L
    private val pokes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun poke() {
        val now = SystemClock.uptimeMillis()
        if (now - lastPoke < POKE_GAP_MS) return
        lastPoke = now
        pokes.tryEmit(Unit)
    }

    suspend fun run(reduceMotion: Boolean) {
        pokes.onStart { emit(Unit) }.collectLatest {
            val wasHidden = !interactive
            alpha.animateTo(1f, tween(if (reduceMotion) 0 else SHOW_MS))
            if (wasHidden) delay(WAKE_GUARD_MS)
            interactive = true
            delay(IDLE_MS)
            interactive = false
            alpha.animateTo(0f, tween(if (reduceMotion) 0 else HIDE_MS))
        }
    }
}

@Composable
internal fun rememberArrowWake(frameKey: Any?, reduceMotion: Boolean): ArrowWake {
    val wake = remember { ArrowWake() }
    LaunchedEffect(wake, reduceMotion) { wake.run(reduceMotion) }
    LaunchedEffect(frameKey) { wake.poke() }
    return wake
}

internal fun Modifier.wakeOnTouch(wake: ArrowWake): Modifier = pointerInput(wake) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.any { it.pressed }) wake.poke()
        }
    }
}
