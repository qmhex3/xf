package com.frameout.app.batch

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frameout.app.FrameOutApp
import com.frameout.core.db.ProjectEntity
import com.frameout.core.db.progressTotal
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec
import com.frameout.engine.extract.ExtractRequest
import com.frameout.engine.timeline.TrimRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FormatProbe
import com.frameout.engine.decode.AnimatedFrameSource
import com.frameout.engine.decode.ApngFrameDecoder
import com.frameout.engine.decode.FfmpegFrameSource
import com.frameout.engine.decode.GifFrameDecoder
import com.frameout.engine.decode.WebpFrameDecoder
import com.frameout.engine.work.ExtractScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BatchViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as FrameOutApp).container
    private val _state = MutableStateFlow(BatchState())
    val state: StateFlow<BatchState> = _state.asStateFlow()
    private var pollJob: Job? = null
    val prefs get() = container.prefs

    private val metaGate = Semaphore(2)
    private val _prep = MutableStateFlow<List<PrepItem>>(emptyList())
    val prep: StateFlow<List<PrepItem>> = _prep.asStateFlow()
    private val _starting = MutableStateFlow(false)
    val starting: StateFlow<Boolean> = _starting.asStateFlow()

    private var session: Long? = null

    fun prepare(uris: List<String>, sessionId: Long) {
        if (_starting.value || session == sessionId) return
        session = sessionId
        pollJob?.cancel()
        pollJob = null
        _state.value = BatchState()
        val seed = uris.distinct().map { PrepItem(it, BatchNaming.displayName(getApplication(), it)) }
        _prep.value = seed
        seed.forEach { item ->
            viewModelScope.launch {
                val meta = withContext(Dispatchers.IO) { metaGate.withPermit { loadMeta(item.uri) } }
                val cover = withContext(Dispatchers.IO) { com.frameout.core.media.MediaThumbLoader.loadForUri(getApplication(), item.uri) }
                _prep.update { list -> list.map { if (it.uri == item.uri) it.copy(durationMs = meta.first, thumb = cover ?: meta.second, loaded = true) else it } }
                val probe = com.frameout.app.trim.SourceProbe.load(getApplication(), Uri.parse(item.uri))
                if (probe != null) _prep.update { list -> list.map { if (it.uri == item.uri) it.copy(probe = probe) else it } }
            }
        }
    }

    fun remove(uri: String) {
        _prep.update { list -> list.filterNot { it.uri == uri } }
    }

    fun setRange(uri: String, range: TrimRange?) {
        _prep.update { list -> list.map { if (it.uri == uri) it.copy(range = range) else it } }
    }

    fun hasSpace(frames: Int): Boolean {
        val autoSave = container.prefs.autoSave.value
        return container.storage.hasSpaceForResolution(frames, 0, 0, autoSave, container.prefs.saveFormat.value)
    }

    private fun loadMeta(uri: String): Pair<Long, Bitmap?> {
        val primary = loadViaRetriever(uri)
        if (primary.first > 0L && primary.second != null) return primary
        val fallback = loadViaDecoder(uri)
        return (if (primary.first > 0L) primary.first else fallback.first) to (primary.second ?: fallback.second)
    }

    private fun loadViaRetriever(uri: String): Pair<Long, Bitmap?> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(getApplication<Application>(), Uri.parse(uri))
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val raw = retriever.getFrameAtTime((duration / 10L).coerceAtMost(1_000L) * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            duration to raw?.let(::scaleThumb)
        } catch (e: Exception) {
            0L to null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun loadViaDecoder(uri: String): Pair<Long, Bitmap?> {
        val context = getApplication<Application>()
        val parsed = Uri.parse(uri)
        val decoder: AnimatedFrameSource = when (runCatching { FormatProbe.probe(context, parsed).kind }.getOrNull()) {
            FormatKind.Gif -> GifFrameDecoder(context)
            FormatKind.Apng -> ApngFrameDecoder(context)
            FormatKind.AnimatedWebp -> WebpFrameDecoder(context)
            else -> FfmpegFrameSource(context)
        }
        return try {
            decoder.open(parsed)
            val durationMs = decoder.durationUs() / 1000L
            val frame = decoder.decodeNearest((durationMs / 10L).coerceAtMost(1_000L) * 1000L)?.second
            durationMs to frame?.let(::scaleThumb)
        } catch (e: Exception) {
            0L to null
        } finally {
            runCatching { decoder.close() }
        }
    }

    private fun scaleThumb(bmp: Bitmap): Bitmap {
        val scale = THUMB_EDGE.toFloat() / maxOf(bmp.width, bmp.height).coerceAtLeast(1)
        if (scale >= 1f) return bmp
        val out = Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt().coerceAtLeast(1), (bmp.height * scale).toInt().coerceAtLeast(1), true)
        if (out !== bmp) bmp.recycle()
        return out
    }

    fun start() {
        val items = _prep.value
        if (items.isEmpty() || _starting.value) return
        _starting.value = true
        viewModelScope.launch {
            val batchId = System.currentTimeMillis().toString()
            val mode = ExtractModeCodec.decode(container.prefs.extractMode.value)
            val autoSave = container.prefs.autoSave.value
            val saveFormat = container.prefs.saveFormat.value
            val jpegQuality = container.prefs.jpegQuality.value
            val created = mutableListOf<BatchItem>()
            val folders = mutableSetOf<String>()
            val requests = items.map { createRequest(it, mode, autoSave, saveFormat, jpegQuality, created, folders) }
            _state.value = BatchState(batchId = batchId, items = created)
            ExtractScheduler.enqueueBatch(getApplication(), requests, batchId)
            _starting.value = false
            poll(created.map { it.projectId })
        }
    }

    private suspend fun createRequest(
        prep: PrepItem,
        mode: ExtractMode,
        autoSave: Boolean,
        saveFormat: String,
        jpegQuality: Int,
        items: MutableList<BatchItem>,
        folders: MutableSet<String>
    ): ExtractRequest {
        val uri = prep.uri
        val name = prep.name
        val range = prep.range?.takeIf { !it.useFull && prep.durationMs > 0 }?.clamped(prep.durationMs)
        val startMs = range?.startMs ?: 0L
        val endMs = range?.endMs ?: 0L
        val folder = BatchNaming.uniqueFolder(name, folders)
        val now = System.currentTimeMillis()
        val id = container.projects.insert(
            ProjectEntity(
                name = name,
                sourceUri = uri,
                sourceName = name,
                status = "Queued",
                createdAt = now,
                updatedAt = now,
                extractMode = ExtractModeCodec.encode(mode),
                autoSave = autoSave,
                saveFormat = saveFormat,
                jpegQuality = jpegQuality,
                saveSubfolder = folder,
                startMs = startMs,
                endMs = endMs
            )
        )
        com.frameout.app.ProjectCover.create(getApplication(), container.storage, container.projects, id, Uri.parse(uri), startMs)
        items += BatchItem(id, name, uri, "Queued")
        return ExtractRequest(id, uri, startMs, endMs, mode, autoSave, saveFormat, jpegQuality)
    }

    fun cancelAll() {
        val batchId = _state.value.batchId
        if (batchId.isNotEmpty()) {
            ExtractScheduler.cancelBatch(getApplication(), batchId)
        } else {
            _state.value.items.filter { !it.finished }.forEach {
                ExtractScheduler.cancel(getApplication(), it.projectId)
            }
        }
    }

    private fun poll(ids: List<Long>) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                val liveValues = com.frameout.engine.work.LiveExtractProgress.all.value
                val updated = ids.map { id ->
                    val p = container.projects.getById(id)
                    BatchItem(
                        projectId = id,
                        name = p?.name ?: "?",
                        uri = p?.sourceUri ?: "",
                        status = p?.status ?: "Unknown",
                        extracted = p?.extractedCount ?: 0,
                        total = p?.progressTotal ?: 0,
                        live = liveValues[id]
                    )
                }
                val done = updated.none { it.status == "Queued" || it.status == "Extracting" || it.status == "Indexing" }
                _state.update {
                    it.copy(items = updated, done = done && updated.isNotEmpty())
                }
                if (done && updated.isNotEmpty()) break
                delay(800)
            }
        }
    }
}

private const val THUMB_EDGE = 360
