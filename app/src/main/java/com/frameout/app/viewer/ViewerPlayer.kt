package com.frameout.app.viewer

import android.os.SystemClock
import com.frameout.core.db.FrameDao
import com.frameout.core.db.FrameEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ViewerPlayer(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<ViewerState>,
    private val frames: FrameDao,
    private val showNext: suspend (FrameEntity) -> Unit,
    private val afterStop: suspend () -> Unit
) {
    private var job: Job? = null

    fun stop() {
        job?.cancel()
    }

    fun toggle() = if (state.value.playing) stop() else start()

    private fun start() {
        val current = state.value
        if (current.current == null || current.editing || job?.isActive == true) return
        state.update { it.copy(playing = true, controlsVisible = false) }
        job = scope.launch {
            try {
                while (true) {
                    val started = SystemClock.elapsedRealtime()
                    val frame = state.value.current ?: break
                    val next = frames.nextActive(frame.projectId, frame.index) ?: break
                    showNext(next)
                    val wait = 1000L / state.value.playFps.coerceAtLeast(1) - (SystemClock.elapsedRealtime() - started)
                    if (wait > 0L) delay(wait)
                }
            } finally {
                state.update { it.copy(playing = false, controlsVisible = true) }
                scope.launch { afterStop() }
            }
        }
    }
}
