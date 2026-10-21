package com.frameout.app.viewer

import androidx.lifecycle.viewModelScope
import com.frameout.core.util.SavedStrings
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun ViewerViewModel.deleteSaved() {
    stopPlay()
    val id = replaceSavedId ?: return
    val index = savedIndex
    viewModelScope.launch { handleTrash(savedDelete.trash(id, index)) }
}

internal fun ViewerViewModel.onSavedConsent(approved: Boolean) {
    viewModelScope.launch {
        _state.update { it.copy(savedConsent = null) }
        savedDelete.consent(approved)?.let { handleTrash(it) }
    }
}

internal fun ViewerViewModel.undoLast() {
    if (_state.value.savedUndoCount > 0) undoSavedDelete() else undoDelete()
}

private suspend fun ViewerViewModel.handleTrash(outcome: SavedDeleteOutcome) {
    when (outcome) {
        is SavedDeleteOutcome.Consent -> _state.update { it.copy(savedConsent = SavedConsent(outcome.sender)) }
        SavedDeleteOutcome.Failed -> _state.update { it.copy(message = SavedStrings.deleteFailed(), busyMessage = null, messageSaved = null) }
        is SavedDeleteOutcome.Done -> {
            _state.update { it.copy(savedUndoCount = savedDelete.pending, message = if (outcome.undoable) it.message else SavedStrings.deleted(), busyMessage = null) }
            showAfterRemoval(outcome.id)
        }
    }
}

private suspend fun ViewerViewModel.showAfterRemoval(id: Long) {
    val removedAt = savedIds.indexOf(id)
    val currentId = replaceSavedId
    savedIds = savedIds.filter { it != id }
    SavedPlaylist.ids = savedIds
    if (currentId != id) {
        savedIndex = currentId?.let { savedIds.indexOf(it) }?.coerceAtLeast(0) ?: 0
        return
    }
    val from = removedAt.coerceAtLeast(0)
    val forward = savedFrameFrom(from - 1, 1)
    val target = forward ?: savedFrameFrom(from, -1)
    if (target == null) {
        replaceSavedId = null
        replaceFrameId = null
        loadFrame(null)
        return
    }
    direction = if (forward != null) 1 else -1
    savedIndex = target.first
    replaceSavedId = savedIds[target.first]
    replaceFrameId = target.second.id
    loadFrame(target.second)
}

private fun ViewerViewModel.undoSavedDelete() {
    stopPlay()
    viewModelScope.launch {
        val restored = savedDelete.restore()
        _state.update { it.copy(savedUndoCount = savedDelete.pending) }
        if (restored == null) {
            _state.update { it.copy(message = SavedStrings.restoreFailed(), busyMessage = null, messageSaved = null) }
            return@launch
        }
        val at = restored.index.coerceIn(0, savedIds.size)
        savedIds = savedIds.toMutableList().apply { add(at, restored.id) }
        SavedPlaylist.ids = savedIds
        val frame = c.savedImages.getById(restored.id)?.sourceFrameId?.let { c.frames.getActiveById(it) }
        savedIndex = at
        replaceSavedId = restored.id
        replaceFrameId = frame?.id
        loadFrame(frame)
    }
}
