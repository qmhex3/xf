package com.frameout.app.viewer

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun ViewerViewModel.softDelete() {
    stopPlay()
    if (_state.value.savedMode) { deleteSaved(); return }
    val frame = _state.value.current ?: return
    viewModelScope.launch {
        c.editHistory.delete(frame.id)
        _state.update { it.copy(deletedStack = it.deletedStack + frame.id) }
        val next = c.frames.nextActive(frame.projectId, frame.index) ?: c.frames.previousActive(frame.projectId, frame.index)
        loadFrame(next)
    }
}
internal fun ViewerViewModel.undoDelete() {
    val id = _state.value.deletedStack.lastOrNull() ?: return
    stopPlay()
    viewModelScope.launch {
        if (c.editHistory.undo(id) == null) c.editHistory.restore(id)
        _state.update { it.copy(deletedStack = it.deletedStack.dropLast(1)) }
        loadFrame(c.frames.getActiveById(id))
    }
}
