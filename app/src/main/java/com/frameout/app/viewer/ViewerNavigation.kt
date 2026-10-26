package com.frameout.app.viewer

import androidx.lifecycle.viewModelScope
import com.frameout.core.db.FrameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun ViewerViewModel.refreshPosition(frame: FrameEntity) {
    if (savedIds.isNotEmpty()) {
        _state.update { if (it.current?.id == frame.id) it.copy(position = savedIndex + 1, total = savedIds.size) else it }
        return
    }
    viewModelScope.launch {
        val (before, total) = withContext(Dispatchers.IO) {
            c.frames.countActiveBefore(frame.projectId, frame.index) to c.frames.countActive(frame.projectId)
        }
        _state.update { if (it.current?.id == frame.id) it.copy(position = before + 1, total = total) else it }
    }
}
internal suspend fun ViewerViewModel.refreshNeighbors() {
    val frame = _state.value.current ?: return
    val neighbors = neighborsFor(frame)
    if (_state.value.current?.id != frame.id || _state.value.playing) return
    _state.update { it.withNeighbors(neighbors) }
}
internal suspend fun ViewerViewModel.neighborsFor(frame: FrameEntity): ViewerNeighbors {
    if (savedIds.isEmpty()) return ViewerNeighbors.load(c.frames, editHistory, frame, screenPx)
    val (previous, next) = withContext(Dispatchers.IO) { savedFrameFrom(savedIndex, -1)?.second to savedFrameFrom(savedIndex, 1)?.second }
    return ViewerNeighbors.of(editHistory, previous, next, screenPx)
}

internal suspend fun ViewerViewModel.savedFrameFrom(from: Int, dir: Int): Pair<Int, FrameEntity>? {
    var i = from + dir
    while (i in savedIds.indices) {
        val frame = c.savedImages.getById(savedIds[i])?.sourceFrameId?.let { c.frames.getActiveById(it) }
        if (frame != null) return i to frame
        i += dir
    }
    return null
}

internal suspend fun ViewerViewModel.stepSaved(steps: Int) {
    val dir = if (steps > 0) 1 else -1
    var target: Pair<Int, FrameEntity>? = null
    var from = savedIndex
    repeat(kotlin.math.abs(steps)) {
        val next = withContext(Dispatchers.IO) { savedFrameFrom(from, dir) } ?: return@repeat
        target = next
        from = next.first
    }
    val (index, frame) = target ?: return
    savedIndex = index
    replaceSavedId = savedIds[index]
    replaceFrameId = frame.id
    loadFrame(frame)
}

internal fun ViewerViewModel.goTo(delta: Int) {
    stopPlay()
    if (delta == 0) return
    pendingSteps += delta
    if (navJob?.isActive == true) return
    navJob = viewModelScope.launch {
        while (pendingSteps != 0) {
            val steps = pendingSteps
            pendingSteps = 0
            if (savedIds.isNotEmpty()) {
                direction = if (steps > 0) 1 else -1
                stepSaved(steps)
                continue
            }
            val base = _state.value.current ?: break
            direction = if (steps > 0) 1 else -1
            val target = withContext(Dispatchers.IO) {
                if (steps > 0) c.frames.nextActiveList(base.projectId, base.index, steps)
                else c.frames.previousActiveList(base.projectId, base.index, -steps)
            }.lastOrNull()
            if (target != null) loadFrame(target)
        }
    }
}
