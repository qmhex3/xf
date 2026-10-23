package com.frameout.app.library

import com.frameout.app.IncomingMedia
import com.frameout.core.db.deleteManyChunked
import com.frameout.core.db.getByIdsChunked
import com.frameout.app.deletionMessage
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.core.util.TrimStrings
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import com.frameout.core.storage.SaveFailure
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import com.frameout.app.FrameOutApp
import com.frameout.core.backup.BackupExporter
import com.frameout.core.db.SavedKind
import com.frameout.core.backup.BackupFormat
import com.frameout.core.backup.BackupImporter
import com.frameout.core.backup.BackupResult
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.FrameStrings
import com.frameout.engine.work.ExtractScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import com.frameout.core.storage.GalleryBackup
import com.frameout.core.storage.SavedReconciler
import com.frameout.core.storage.SavedTrash
import com.frameout.core.util.SavedStrings
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as FrameOutApp).container
    private val exporter = FrameExporter(app, container.storage, container.savedImages, container.frameEdits, container.projects, container.videoIndex)
    private val covers = CoverBackfill(app as FrameOutApp, viewModelScope)
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()
    val listState = androidx.compose.foundation.lazy.LazyListState()
    val gridState = androidx.compose.foundation.lazy.grid.LazyGridState()

    init {
        viewModelScope.launch { container.notice.collect { text -> if (text != null) { container.notice.value = null; _state.update { it.copy(message = text) } } } }
        viewModelScope.launch { container.savedImages.observeCount().collect { n -> _state.update { it.copy(savedCount = n) } } }
        viewModelScope.launch { container.savedImages.observeAlbums().collect { list -> _state.update { it.copy(albums = list, selectedAlbum = it.selectedAlbum?.takeIf { a -> list.any { row -> row.album == a } }) } } }
        viewModelScope.launch {
            container.savedImages.observeKindCounts().collect { rows ->
                val counts = rows.mapNotNull { row -> SavedKind.values().firstOrNull { it.code == row.kind }?.let { it to row.total } }.toMap()
                _state.update { it.copy(savedKindCounts = counts, savedKind = it.savedKind?.takeIf { kind -> (counts[kind] ?: 0) > 0 }) }
            }
        }
        viewModelScope.launch { container.projects.observeAll().collect { list -> rawProjects = list; covers.run(list); publishProjects(); hintBackup(list) } }
    }
    private fun hintBackup(list: List<com.frameout.core.db.ProjectEntity>) {
        val prefs = container.prefs
        if (prefs.backupHintShown() || list.none { it.status == com.frameout.core.model.ProjectStatus.Completed.name }) return
        prefs.markBackupHintShown()
        container.notice.value = com.frameout.core.util.FrameStrings.backupHint()
    }
    fun setTab(tab: LibraryTab) = _state.update { it.copy(tab = tab, selectedProjects = emptySet(), selectedSaved = emptySet()) }
    fun setAlbum(album: String?) = _state.update { it.copy(selectedAlbum = album, selectedSaved = emptySet()) }
    fun setSavedKind(kind: SavedKind?) = _state.update { it.copy(savedKind = kind, selectedSaved = emptySet()) }
    fun setQuery(query: String) = _state.update { it.copy(query = query, selectedSaved = emptySet()) }
    fun setProjectQuery(query: String) = _state.update { it.withProjectQuery(query) }
    fun setProjectSort(sort: LibrarySort) = _state.update { it.copy(projectSort = sort) }
    fun setSavedSort(sort: LibrarySort) = _state.update { it.copy(savedSort = sort) }
    fun setColumns(columns: Int) = _state.update { it.copy(columns = columns.coerceIn(2, 4)) }
    fun clearMessage() = _state.update { it.copy(message = null, messageSaved = null, messageIcon = null, errorMessage = null) }
    fun clearFlash() = _state.update { it.copy(flashSuccess = false) }
    fun pauseFocused() { _state.value.focusId?.let { ExtractScheduler.pause(getApplication(), it) } }
    fun pause(projectId: Long) { ExtractScheduler.pause(getApplication(), projectId) }
    fun resumeFocused() { _state.value.focusId?.let { resume(it) } }
    fun cancelFocused() { _state.value.focusId?.let { ExtractScheduler.cancel(getApplication(), it) } }
    fun toggleProject(id: Long) = _state.update { it.toggleProjectSelection(id) }
    fun toggleSaved(id: Long) = _state.update { it.toggleSavedSelection(id) }
    fun clearSelection() = _state.update { it.copy(selectedProjects = emptySet(), selectedSaved = emptySet()) }
    fun delete(projectId: Long) { queueProjectDelete(listOf(projectId)) }
    fun importBackup(uri: android.net.Uri) = viewModelScope.launch {
        val result = BackupImporter(getApplication(), container).import(uri)
        _state.update { it.withMessage(result.message(), result != BackupResult.Imported && result != BackupResult.Duplicate, FrameIconsExtra.Import) }
    }
    fun exportToDocument(uri: android.net.Uri) = viewModelScope.launch {
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

    fun exportToFolder(folder: android.net.Uri) = viewModelScope.launch {
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

    private fun exportRunning() = _state.value.exportProgress != null
    fun notifyExportBusy() = _state.update { it.copy(message = FrameStrings.exportBusy(), messageSaved = null) }
    fun clearExport() = _state.update { it.copy(exportSaved = null, exportNote = null) }
    fun cancelExport() { exportJob?.cancel(); savedActions.job?.cancel() }
    private val savedActions by lazy { SavedLibraryActions(viewModelScope, getApplication(), container, exporter, _state) }
    fun saveSelectedToGallery() = savedActions.saveToGallery()
    internal fun exportSelectedSaved(kind: SavedExportKind, tree: android.net.Uri?, contact: com.frameout.core.export.ContactSheetLayout.Options? = null) = savedActions.export(kind, tree, contact)

    fun toggleSelectAll() = viewModelScope.launch {
        val current = _state.value
        if (current.tab == LibraryTab.Projects) {
            val all = current.visibleProjects.mapTo(HashSet()) { it.id }
            _state.update { it.copy(selectedProjects = if (it.selectedProjects == all) emptySet() else all) }
        } else {
            val all = container.savedImages.filteredIds(current.selectedAlbum, escapeLike(current.query.trim()), current.savedKind?.code ?: -1).toHashSet()
            _state.update { it.copy(selectedSaved = if (it.selectedSaved == all) emptySet() else all) }
        }
    }

    private var exportJob: kotlinx.coroutines.Job? = null
    private val frameExport by lazy { com.frameout.app.frames.FramesExportAction(getApplication<Application>()) }

    internal fun exportProjects(kind: ProjectExportKind) {
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

    private suspend fun exportOne(kind: ProjectExportKind, id: Long, tally: ExportTally) {
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

    private suspend fun activeFramesOf(projectId: Long): List<com.frameout.core.db.FrameEntity> {
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

    private fun finishExport(t: ExportTally, total: Int) {
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

    fun openSaved(savedId: Long, onViewer: (projectId: Long, frameId: Long) -> Unit, onSystem: (android.net.Uri) -> Unit) = viewModelScope.launch {
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
            if (saved.sourceFrameId != null) _state.update { it.withMessage(FrameStrings.savedOpenSystem(), false, FrameIconsExtra.Album) }
            onSystem(uri)
        } else {
            _state.update { it.withMessage(FrameStrings.savedMissing(), true) }
            reconcileSaved(force = true)
        }
    }

    fun deleteSelectedProjects() {
        val ids = _state.value.selectedProjects.toList()
        if (ids.isEmpty()) return
        clearSelection()
        queueProjectDelete(ids)
    }

    private var rawProjects: List<com.frameout.core.db.ProjectEntity> = emptyList()
    private val pendingProjectDeletes = LinkedHashSet<Long>()
    private val committingProjects = HashSet<Long>()
    private var projectUndoJob: Job? = null

    private fun publishProjects() {
        val shown = rawProjects.filter { it.id !in pendingProjectDeletes }
        _state.update { it.withProjects(shown) }
    }

    private fun undoableProjects(): List<Long> = pendingProjectDeletes.filter { it !in committingProjects }

    private fun queueProjectDelete(ids: Collection<Long>) {
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

    private suspend fun commitProjectDeletes() {
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

    private suspend fun removeProjects(ids: List<Long>): Int {
        val stopped = kotlinx.coroutines.coroutineScope {
            ids.map { id -> async { id to ExtractScheduler.cancelAndAwait(getApplication(), id, DELETE_STOP_MS) } }.awaitAll().filter { it.second }.map { it.first }
        }
        return stopped.count { id -> attempt { deleteProjectRecordAfterStorage(id) } }
    }

    fun undoProjectDelete() {
        val restore = undoableProjects()
        if (restore.isEmpty()) return
        projectUndoJob?.cancel()
        pendingProjectDeletes.removeAll(restore.toSet())
        publishProjects()
        _state.update { it.withMessage(FrameStrings.projectsRestored(restore.size), false, FrameIcons.Trash).copy(undoProjectCount = 0) }
    }

    private val trash by lazy { SavedTrash(container.savedImages, GalleryBackup(getApplication())) }
    private var undoJob: Job? = null

    fun deleteSelectedSaved() = viewModelScope.launch {
        val ids = _state.value.selectedSaved.toList()
        if (ids.isEmpty()) return@launch
        undoJob?.cancel()
        _state.update { it.copy(undoSavedCount = 0) }
        val saved = container.savedImages.getByIdsChunked(ids)
        trash.begin(saved)
        val removable = saved.filter { container.storage.deleteGalleryUri(it.uri) }
        if (removable.isNotEmpty()) container.savedImages.deleteManyChunked(removable.map { it.id })
        trash.markGone(removable)
        val removedIds = removable.map { it.id }.toSet()
        val failed = saved.filter { it.id !in removedIds }
        clearSelection()
        val sender = if (failed.isEmpty()) null else withContext(Dispatchers.IO) { container.storage.galleryDeleteRequest(failed.map { it.uri }) }
        if (sender != null) {
            _state.update { it.copy(pendingGalleryDelete = PendingGalleryDelete(sender, failed.map { f -> f.id }, removable.size)) }
        } else {
            finishSavedDelete(removable.size, ids.size - removable.size)
        }
    }

    private fun finishSavedDelete(removed: Int, failed: Int) {
        trash.release()
        val undo = trash.undoable
        _state.update { it.withMessage(deletionMessage(removed, failed, saved = true), failed > 0, FrameIcons.Trash).copy(undoSavedCount = undo) }
        if (undo > 0) {
            undoJob?.cancel()
            undoJob = viewModelScope.launch {
                delay(UNDO_WINDOW_MS)
                trash.commit()
                _state.update { it.copy(undoSavedCount = 0) }
            }
        }
    }

    fun undoSavedDelete() {
        if (_state.value.undoSavedCount == 0) return
        undoJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(undoSavedCount = 0, message = null, errorMessage = null, messageIcon = null, messageSaved = null) }
            val restored = trash.restoreAll()
            _state.update {
                if (restored > 0) it.withMessage(SavedStrings.restored(restored), false, FrameIcons.Trash)
                else it.withMessage(SavedStrings.restoreFailed(), true)
            }
        }
    }

    private var lastReconcile = 0L
    private var reconcileJob: Job? = null
    private val reconciler by lazy { SavedReconciler(container.storage, container.savedImages, container.frames) }

    fun reconcileSaved(force: Boolean = false) {
        val now = android.os.SystemClock.elapsedRealtime()
        val s = _state.value
        if (reconcileJob?.isActive == true || s.exportProgress != null || s.pendingGalleryDelete != null || s.undoSavedCount > 0) return
        if (!force && lastReconcile != 0L && now - lastReconcile < RECONCILE_MIN_MS) return
        lastReconcile = now
        reconcileJob = viewModelScope.launch {
            val removed = try {
                reconciler.prune()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                com.frameout.core.util.DiagLog.append("library", "reconcile failed: ${e.javaClass.simpleName}: ${e.message}")
                0
            }
            if (removed > 0 && _state.value.undoSavedCount == 0) _state.update { it.withMessage(SavedStrings.pruned(removed), false, FrameIcons.Trash) }
        }
    }

    override fun onCleared() {
        val rest = undoableProjects()
        if (rest.isNotEmpty()) {
            committingProjects.addAll(rest)
            getApplication<FrameOutApp>().appScope.launch { removeProjects(rest) }
        }
        trash.commit()
        super.onCleared()
    }
    fun shareSelectedSaved(onReady: (android.content.Intent) -> Unit) = viewModelScope.launch {
        val ids = _state.value.selectedSaved.toList()
        if (ids.isEmpty()) return@launch
        val limit = FrameExporter.MAX_SHARE_FRAMES
        if (ids.size > limit) _state.update { it.withMessage(FrameStrings.shareLimited(limit), false, FrameIcons.Share) }
        val saved = container.savedImages.getByIdsChunked(ids.take(limit))
        val resolver = getApplication<Application>().contentResolver
        val shareable = withContext(Dispatchers.IO) {
            saved.mapNotNull { item ->
                val uri = android.net.Uri.parse(item.uri)
                val type = runCatching { resolver.getType(uri) }.getOrNull() ?: return@mapNotNull null
                runCatching { resolver.openFileDescriptor(uri, "r")?.use { uri to type } }.getOrNull()
            }
        }
        if (shareable.isEmpty()) {
            _state.update { it.withMessage(FrameStrings.savedMissing(), true) }
            return@launch
        }
        val uris = ArrayList(shareable.map { it.first })
        val types = shareable.map { it.second }.distinct()
        val mime = when {
            types.size == 1 -> types.first()
            types.map { it.substringBefore('/') }.distinct().size == 1 -> types.first().substringBefore('/') + "/*"
            else -> "*/*"
        }
        val intent = if (uris.size == 1) {
            android.content.Intent(android.content.Intent.ACTION_SEND).putExtra(android.content.Intent.EXTRA_STREAM, uris.first())
        } else {
            android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
        }
        intent.type = mime
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = android.content.ClipData.newRawUri("", uris.first()).also { clip -> uris.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it)) } }
        onReady(intent)
    }
    fun onGalleryDeleteResult(approved: Boolean) = viewModelScope.launch {
        val pending = _state.value.pendingGalleryDelete ?: return@launch
        _state.update { it.copy(pendingGalleryDelete = null) }
        val all = container.savedImages.getByIdsChunked(pending.ids)
        val gone = withContext(Dispatchers.IO) { all.filter { container.storage.deleteGalleryUri(it.uri) } }
        if (gone.isNotEmpty()) container.savedImages.deleteManyChunked(gone.map { it.id })
        trash.markGone(gone)
        val goneIds = gone.map { it.id }.toSet()
        val remaining = all.filter { it.id !in goneIds }
        val removed = pending.alreadyRemoved + gone.size
        if (approved && gone.isNotEmpty() && remaining.isNotEmpty()) {
            val next = withContext(Dispatchers.IO) { container.storage.galleryDeleteRequest(remaining.map { it.uri }) }
            if (next != null) {
                _state.update { it.copy(pendingGalleryDelete = PendingGalleryDelete(next, remaining.map { r -> r.id }, removed)) }
                return@launch
            }
        }
        val total = pending.alreadyRemoved + pending.ids.size
        finishSavedDelete(removed, total - removed)
    }
    fun resume(projectId: Long) = viewModelScope.launch {
        if (container.projects.getById(projectId) != null) ExtractScheduler.resume(getApplication(), projectId).join()
    }
    fun resave(savedId: Long) = viewModelScope.launch {
        val saved = container.savedImages.getById(savedId) ?: return@launch
        val frame = saved.sourceFrameId?.let { container.frames.getById(it) }
        if (frame == null) { _state.update { it.withMessage(if (saved.sourceFrameId == null) FrameStrings.noSourceFrame() else FrameStrings.sourceGone(), true) }; return@launch }
        val ok = exporter.resaveFromFrame(savedId, frame)
        _state.update { it.withMessage(if (ok) FrameStrings.savedAgain() else SaveFailure.message(getApplication<Application>(), FrameStrings.resaveFailed()), !ok).copy(messageSaved = if (ok) com.frameout.designsystem.icons.SaveKind.Image else null) }
    }

    private suspend fun attempt(block: suspend () -> Boolean): Boolean = try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        com.frameout.core.util.DiagLog.append("library", "delete failed: ${e.javaClass.simpleName}: ${e.message}")
        false
    }

    private suspend fun deleteProjectRecordAfterStorage(id: Long): Boolean {
        val project = container.projects.getById(id) ?: return true
        val storageDeleted = withContext(Dispatchers.IO) { container.storage.deleteProject(id) }
        if (!storageDeleted) return false
        container.projects.delete(id)
        com.frameout.core.util.FailureMarker.clear(getApplication(), id)
        withContext(Dispatchers.IO) {
            runCatching {
                IncomingMedia.discardSource(getApplication(), project.sourceUri, container.projects.allSourceUris() + container.prefs.lastVideoUri())
            }
        }
        return true
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val savedPagingFlow: Flow<PagingData<SavedListItem>> = _state
        .map { SavedQuery(it.selectedAlbum, it.query.trim(), it.savedSort, it.savedKind?.code ?: -1) }
        .distinctUntilChanged()
        .debounce(200)
        .flatMapLatest { (album, query, sort, kind) ->
            Pager(PagingConfig(36, prefetchDistance = 18, enablePlaceholders = false)) {
                container.savedImages.pagingSorted(album, escapeLike(query), sort.savedOrder(), kind)
            }.flow.map { data ->
                data.map { SavedListItem.Image(it) }.insertSeparators { before, after ->
                    val title = after?.item?.album?.ifBlank { FrameStrings.appName() } ?: return@insertSeparators null
                    val previous = before?.item?.album?.ifBlank { FrameStrings.appName() }
                    if (previous != title) SavedListItem.Header(title) else null
                }
            }
        }.cachedIn(viewModelScope)
}

private data class SavedQuery(val album: String?, val query: String, val sort: LibrarySort, val kind: Int)

private fun LibrarySort.savedOrder(): Int = when (this) { LibrarySort.Oldest -> 1; LibrarySort.Name -> 2; else -> 0 }

private fun escapeLike(query: String): String = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

private fun LibraryState.toggleProjectSelection(id: Long): LibraryState = copy(selectedProjects = selectedProjects.toMutableSet().also { if (!it.add(id)) it.remove(id) })
private fun LibraryState.toggleSavedSelection(id: Long): LibraryState = copy(selectedSaved = selectedSaved.toMutableSet().also { if (!it.add(id)) it.remove(id) })

private const val DELETE_STOP_MS = 15_000L
private const val UNDO_WINDOW_MS = 6_000L
private const val RECONCILE_MIN_MS = 60_000L
private val LIVE_STATUSES = setOf("Extracting", "Indexing", "Queued")
