package com.frameout.app.picker

import com.frameout.core.util.FrameStrings
import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.frameout.core.media.FolderMediaIndex
import com.frameout.core.media.MediaKind
import com.frameout.core.media.MediaItem
import com.frameout.core.media.MediaScanner
import com.frameout.core.media.PermissionHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PickerViewModel(app: Application) : AndroidViewModel(app) {
    private val scanner = MediaScanner(app)
    private val query = MutableStateFlow(Query(null, null))
    private var summaryJob: Job? = null
    private val _state = MutableStateFlow(PickerState())
    val state: StateFlow<PickerState> = _state.asStateFlow()
    val paging: Flow<PagingData<MediaItem>> = query.flatMapLatest { q ->
        Pager(PagingConfig(pageSize = 60, initialLoadSize = 60, prefetchDistance = 120, enablePlaceholders = false)) { scanner.pagingSource(q.bucketId, q.kind) }.flow
    }.cachedIn(viewModelScope)

    @Volatile private var dirty = false
    private var loadedOnce = false
    private var lastAccess = accessSignature()
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            dirty = true
        }
    }

    init {
        app.contentResolver.registerContentObserver(MediaStore.Files.getContentUri("external"), true, observer)
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(observer)
    }

    fun refreshPermission(force: Boolean = false) {
        val context = getApplication<Application>()
        val granted = PermissionHelper.hasMediaAccess(context)
        val access = accessSignature()
        val accessChanged = access != lastAccess
        lastAccess = access
        _state.update { it.copy(permissionGranted = granted) }
        if (!granted) return
        viewModelScope.launch { FolderMediaIndex.loadSafely(context) }
        val firstPass = !loadedOnce
        if (!firstPass && !dirty && !accessChanged && !force) return
        val requery = !firstPass || dirty || accessChanged || force
        dirty = false
        loadedOnce = true
        if (requery) scanner.clearSummaryCache()
        load(_state.value.selectedBucket, reloadSummary = true, requery = requery)
    }

    private fun accessSignature(): Int {
        val context = getApplication<Application>()
        return (if (PermissionHelper.canReadVideos(context)) 1 else 0) or
            (if (PermissionHelper.canReadImages(context)) 2 else 0) or
            (if (PermissionHelper.isPartialMediaAccess(context)) 4 else 0)
    }

    fun selectAlbum(bucketId: String?) {
        if (_state.value.selectedBucket == bucketId) return
        _state.update { it.copy(generation = it.generation + 1) }
        load(bucketId)
    }

    fun load(bucketId: String? = _state.value.selectedBucket, reloadSummary: Boolean = false, requery: Boolean = true) {
        val needSummary = reloadSummary || _state.value.albums.isEmpty()
        _state.update { it.copy(loading = needSummary, error = null, selectedBucket = bucketId) }
        if (requery) query.update { it.copy(bucketId = bucketId, kind = _state.value.selectedKind, generation = it.generation + 1) }
        if (!needSummary) return
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            runCatching {
                val summary = scanner.summary()
                val kind = _state.value.selectedKind
                val stillValid = kind == null || summary.filters.any { it.kind == kind }
                _state.update { it.copy(albums = summary.albums, filters = summary.filters, loading = false, permissionGranted = true, selectedKind = if (stillValid) kind else null) }
                if (!stillValid) query.update { it.copy(kind = null, generation = it.generation + 1) }
            }.onFailure { e -> if (e !is kotlinx.coroutines.CancellationException) _state.update { it.copy(loading = false, error = e.message ?: FrameStrings.scanFailed()) } }
        }
    }

    fun setFilter(kind: MediaKind?) {
        if (_state.value.selectedKind == kind) return
        _state.update { it.copy(selectedKind = kind, generation = it.generation + 1) }
        query.update { it.copy(kind = kind) }
    }

    fun toggle(item: MediaItem) = _state.update { it.toggleSelect(item) }

    fun moveSelected(from: Int, to: Int) = _state.update { it.moveSelect(from, to) }

    fun restoreItem(item: MediaItem, index: Int) = _state.update { it.restoreAt(item, index) }

    fun clearSelection() = _state.update { it.copy(selected = linkedMapOf()) }

    private data class Query(val bucketId: String?, val kind: MediaKind?, val generation: Int = 0)
}
