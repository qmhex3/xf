package com.frameout.app.library

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.map
import com.frameout.app.deletionMessage
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.engine.work.ExtractScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun LibraryViewModel.openSaved(savedId: Long, onViewer: (projectId: Long, frameId: Long) -> Unit, onSavedImage: () -> Unit, onSystem: (android.net.Uri) -> Unit) = viewModelScope.launch {
    val saved = container.savedImages.getById(savedId) ?: return@launch
    val frame = saved.sourceFrameId?.let { container.frames.getActiveById(it) }
    if (frame != null) {
        val s = _state.value
        com.frameout.app.viewer.SavedPlaylist.ids = withContext(Dispatchers.IO) {
            container.savedImages.orderedIds(s.selectedAlbum, escapeLike(s.query.trim()), s.savedSort.savedOrder(), s.savedKind?.code ?: -1)
        }
        onViewer(frame.projectId, frame.id)
        return@launch
    }
    val uri = android.net.Uri.parse(saved.uri)
    val exists = withContext(Dispatchers.IO) {
        runCatching { getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false }.getOrDefault(false)
    }
    if (exists) {
        val still = withContext(Dispatchers.IO) {
            runCatching { !SavedMediaDecoder(getApplication<Application>()).isMoving(uri, saved.displayName) }.getOrDefault(false)
        }
        if (still) {
            val s = _state.value
            com.frameout.app.viewer.SavedPlaylist.ids = withContext(Dispatchers.IO) {
                container.savedImages.orderedIds(s.selectedAlbum, escapeLike(s.query.trim()), s.savedSort.savedOrder(), s.savedKind?.code ?: -1)
            }
            onSavedImage()
            return@launch
        }
    }
    if (exists) {
        if (saved.sourceFrameId != null) _state.update { it.withMessage(FrameStrings.savedOpenSystem(), false, FrameIconsExtra.Album) }
        onSystem(uri)
    } else {
        _state.update { it.withMessage(FrameStrings.savedMissing(), true) }
        reconcileSaved(force = true)
    }
}

internal fun LibraryViewModel.deleteSelectedProjects() {
    val ids = _state.value.selectedProjects.toList()
    if (ids.isEmpty()) return
    clearSelection()
    queueProjectDelete(ids)
}
internal fun LibraryViewModel.publishProjects() {
    val shown = rawProjects.filter { it.id !in pendingProjectDeletes }
    _state.update { it.withProjects(shown) }
}

internal fun LibraryViewModel.undoableProjects(): List<Long> = pendingProjectDeletes.filter { it !in committingProjects }

internal fun LibraryViewModel.queueProjectDelete(ids: Collection<Long>) {
    if (exportRunning()) { notifyExportBusy(); return }
    val known = rawProjects.mapTo(HashSet()) { it.id }
    val added = ids.filter { it in known && pendingProjectDeletes.add(it) }
    if (added.isEmpty()) return
    publishProjects()
    val count = undoableProjects().size
    _state.update { it.withMessage(FrameStrings.projectsDeleted(count), false, FrameIcons.Trash).copy(undoProjectCount = count) }
    projectUndoJob?.cancel()
    projectUndoJob = viewModelScope.launch {
        delay(UNDO_WINDOW_MS)
        commitProjectDeletes()
    }
}

internal suspend fun LibraryViewModel.commitProjectDeletes() {
    val ids = undoableProjects()
    if (ids.isEmpty()) return
    committingProjects.addAll(ids)
    val deleted = withContext(kotlinx.coroutines.NonCancellable) { removeProjects(ids) }
    pendingProjectDeletes.removeAll(ids.toSet())
    committingProjects.removeAll(ids.toSet())
    publishProjects()
    val failed = ids.size - deleted
    val remaining = undoableProjects().size
    _state.update {
        val base = it.copy(undoProjectCount = if (failed == 0) remaining else 0)
        if (failed > 0) base.withMessage(deletionMessage(deleted, failed), true, FrameIcons.Trash) else base
    }
}

internal suspend fun LibraryViewModel.removeProjects(ids: List<Long>): Int {
    val stopped = kotlinx.coroutines.coroutineScope {
        ids.map { id -> async { id to ExtractScheduler.cancelAndAwait(getApplication(), id, DELETE_STOP_MS) } }.awaitAll().filter { it.second }.map { it.first }
    }
    return stopped.count { id -> attempt { deleteProjectRecordAfterStorage(id) } }
}

internal fun LibraryViewModel.undoProjectDelete() {
    val restore = undoableProjects()
    if (restore.isEmpty()) return
    projectUndoJob?.cancel()
    pendingProjectDeletes.removeAll(restore.toSet())
    publishProjects()
    _state.update { it.withMessage(FrameStrings.projectsRestored(restore.size), false, FrameIcons.Trash).copy(undoProjectCount = 0) }
}
