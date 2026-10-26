package com.frameout.app.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.frameout.app.FrameOutApp
import com.frameout.app.IncomingMedia
import com.frameout.core.db.SavedKind
import com.frameout.core.db.deleteManyChunked
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.storage.FrameExporter
import com.frameout.core.storage.GalleryBackup
import com.frameout.core.storage.SaveFailure
import com.frameout.core.storage.SavedReconciler
import com.frameout.core.storage.SavedTrash
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.engine.work.ExtractScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    internal val container = (app as FrameOutApp).container
    internal val exporter = FrameExporter(app, container.storage, container.savedImages, container.frameEdits, container.projects, container.videoIndex)
    private val covers = CoverBackfill(app as FrameOutApp, viewModelScope)
    internal val _state = MutableStateFlow(LibraryState())
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

    internal fun exportRunning() = _state.value.exportProgress != null
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

    internal var exportJob: kotlinx.coroutines.Job? = null
    internal val frameExport by lazy { com.frameout.app.frames.FramesExportAction(getApplication<Application>()) }

    internal var rawProjects: List<com.frameout.core.db.ProjectEntity> = emptyList()
    internal val pendingProjectDeletes = LinkedHashSet<Long>()
    internal val committingProjects = HashSet<Long>()
    internal var projectUndoJob: Job? = null

    internal val trash by lazy { SavedTrash(container.savedImages, GalleryBackup(getApplication())) }
    internal var undoJob: Job? = null

    internal var lastReconcile = 0L
    internal var reconcileJob: Job? = null
    internal val reconciler by lazy { SavedReconciler(container.storage, container.savedImages, container.frames) }

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

    internal suspend fun attempt(block: suspend () -> Boolean): Boolean = try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        com.frameout.core.util.DiagLog.append("library", "delete failed: ${e.javaClass.simpleName}: ${e.message}")
        false
    }

    internal suspend fun deleteProjectRecordAfterStorage(id: Long): Boolean {
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

internal fun LibrarySort.savedOrder(): Int = when (this) { LibrarySort.Oldest -> 1; LibrarySort.Name -> 2; else -> 0 }

internal fun escapeLike(query: String): String = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

private fun LibraryState.toggleProjectSelection(id: Long): LibraryState = copy(selectedProjects = selectedProjects.toMutableSet().also { if (!it.add(id)) it.remove(id) })
private fun LibraryState.toggleSavedSelection(id: Long): LibraryState = copy(selectedSaved = selectedSaved.toMutableSet().also { if (!it.add(id)) it.remove(id) })

internal const val DELETE_STOP_MS = 15_000L
internal const val UNDO_WINDOW_MS = 6_000L
internal const val RECONCILE_MIN_MS = 60_000L
internal val LIVE_STATUSES = setOf("Extracting", "Indexing", "Queued")
