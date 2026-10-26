package com.frameout.app.library

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.map
import com.frameout.core.backup.BackupExporter
import com.frameout.core.backup.BackupFormat
import com.frameout.core.backup.BackupImporter
import com.frameout.core.backup.BackupResult
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIconsExtra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun LibraryViewModel.importBackup(uri: android.net.Uri) = viewModelScope.launch {
    val result = BackupImporter(getApplication(), container).import(uri)
    _state.update { it.withMessage(result.message(), result != BackupResult.Imported && result != BackupResult.Duplicate, FrameIconsExtra.Import) }
}
internal fun LibraryViewModel.exportToDocument(uri: android.net.Uri) = viewModelScope.launch {
    val id = _state.value.selectedProjects.singleOrNull() ?: return@launch
    val name = container.projects.getById(id)?.let { BackupFormat.fileName(it.name, System.currentTimeMillis()) }
    val result = BackupExporter(getApplication(), container).exportTo(id, uri)
    if (result == BackupResult.Saved) clearSelection()
    val shown = withContext(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull()
    } ?: name
    _state.update { it.withMessage(if (result == BackupResult.Saved && shown != null) FrameStrings.backupSavedNamed(shown) else result.message(), result != BackupResult.Saved).copy(messageSaved = if (result == BackupResult.Saved) com.frameout.designsystem.icons.SaveKind.File else null) }
}

internal fun LibraryViewModel.exportToFolder(folder: android.net.Uri) = viewModelScope.launch {
    val ids = _state.value.selectedProjects.toList()
    if (ids.isEmpty()) return@launch
    val exporter = BackupExporter(getApplication(), container)
    val results = ids.map { exporter.exportToFolder(it, folder).first }
    val done = results.count { it == BackupResult.Saved }
    if (done > 0) clearSelection()
    val failure = results.firstOrNull { it != BackupResult.Saved }
    val text = when {
        failure == null -> FrameStrings.backupSavedMany(done)
        done == 0 -> failure.message()
        else -> FrameStrings.backupPartial(done, ids.size)
    }
    _state.update { it.withMessage(text, failure != null).copy(messageSaved = if (failure == null) com.frameout.designsystem.icons.SaveKind.File else null) }
}
internal fun LibraryViewModel.exportProjects(kind: ProjectExportKind) {
    if (kind == ProjectExportKind.Backup) return
    if (exportRunning()) { notifyExportBusy(); return }
    val ids = _state.value.selectedProjects.toList()
    if (ids.isEmpty()) return
    exportJob = viewModelScope.launch {
        val tally = ExportTally()
        _state.update { it.copy(exportProgress = ExportProgress(0, ids.size), exportSaved = null, exportNote = null, message = null, messageSaved = null) }
        try {
            ids.forEachIndexed { i, id ->
                exportOne(kind, id, tally)
                _state.update { it.copy(exportProgress = ExportProgress(i + 1, ids.size)) }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            _state.update { it.withMessage(FrameStrings.exportCancelled(), false, FrameIconsExtra.Cancel) }
            throw e
        } finally {
            _state.update { it.copy(exportProgress = null) }
        }
        finishExport(tally, ids.size)
    }
}

private class ExportTally {
    var done = 0
    var failed = 0
    var live = 0
    var empty = 0
    var last: com.frameout.app.trim.SavedCapture? = null
    var failure: String? = null
}

internal suspend fun LibraryViewModel.exportOne(kind: ProjectExportKind, id: Long, tally: ExportTally) {
    val project = container.projects.getById(id) ?: run { tally.failed++; return }
    if (project.status in LIVE_STATUSES) { tally.live++; return }
    val frames = activeFramesOf(id)
    if (frames.isEmpty()) { tally.empty++; return }
    val outcome = if (kind == ProjectExportKind.Zip) frameExport.zip(frames, project.name) else frameExport.video(project, frames)
    val saved = outcome.saved
    if (saved != null) {
        tally.done++
        tally.last = saved
    } else {
        tally.failed++
        tally.failure = outcome.message
    }
}

internal suspend fun LibraryViewModel.activeFramesOf(projectId: Long): List<com.frameout.core.db.FrameEntity> {
    val out = ArrayList<com.frameout.core.db.FrameEntity>()
    var offset = 0
    while (true) {
        val page = container.frames.pageActive(projectId, 500, offset)
        if (page.isEmpty()) break
        out += page
        offset += page.size
    }
    return out
}

internal fun LibraryViewModel.finishExport(t: ExportTally, total: Int) {
    val skipped = t.live + t.empty + t.failed
    if (t.done > 0) clearSelection()
    if (t.done == 1 && skipped == 0) {
        _state.update { it.copy(exportSaved = t.last) }
        return
    }
    val parts = buildList {
        if (t.done > 0) add(if (t.failed == 0 && t.live == 0 && t.empty == 0) FrameStrings.exportDoneMany(t.done) else FrameStrings.backupPartial(t.done, total))
        else if (t.failed > 0) add(t.failure ?: FrameStrings.saveFailed())
        if (t.live > 0) add(FrameStrings.exportSkippedLive(t.live))
        if (t.empty > 0) add(FrameStrings.exportSkippedEmpty(t.empty))
        if (t.done > 0 && t.failed > 0) add(t.failure ?: FrameStrings.saveFailed())
    }
    _state.update { it.withMessage(parts.joinToString(FrameStrings.sep()).ifBlank { FrameStrings.nothingSaved() }, t.failed > 0).copy(messageSaved = if (t.done > 0) com.frameout.designsystem.icons.SaveKind.File else null) }
}
