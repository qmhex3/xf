package com.frameout.app.library

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.frameout.app.trim.SavedCapture
import com.frameout.core.db.SavedImageEntity
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.export.ContactSheetLayout
import com.frameout.core.storage.FileSaver
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.AppContainer
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SavedStrings
import com.frameout.core.util.logFailure
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.icons.SaveKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal enum class SavedExportKind { Zip, Folder, Pdf, Contact }

internal class SavedLibraryActions(
    private val scope: CoroutineScope,
    private val app: Application,
    private val container: AppContainer,
    private val exporter: FrameExporter,
    private val state: MutableStateFlow<LibraryState>
) {
    var job: Job? = null
        private set

    private val resolver get() = app.contentResolver
    private val saver by lazy { FileSaver(app) }
    private val docs by lazy { SavedDocExport(app, container, saver) }

    fun saveToGallery() {
        val ids = state.value.selectedSaved.toList()
        if (ids.isEmpty() || state.value.exportProgress != null) return
        job = scope.launch {
            state.update { it.copy(exportProgress = ExportProgress(0, ids.size), message = null, messageSaved = null) }
            val tally = GalleryTally()
            try {
                val items = container.savedImages.getByIdsChunked(ids)
                items.forEachIndexed { i, item ->
                    tally.add(saveOne(item))
                    state.update { it.copy(exportProgress = ExportProgress(i + 1, ids.size)) }
                }
                tally.failed += ids.size - items.size
            } catch (e: CancellationException) {
                state.update { it.withMessage(FrameStrings.exportCancelled(), false, FrameIconsExtra.Cancel) }
                throw e
            } finally {
                state.update { it.copy(exportProgress = null) }
            }
            val text = tally.message()
            state.update { it.copy(selectedSaved = emptySet(), message = text, errorMessage = text.takeIf { tally.failed > 0 }, messageSaved = if (tally.saved > 0) SaveKind.Image else null) }
        }
    }

    fun export(kind: SavedExportKind, tree: Uri?, contact: ContactSheetLayout.Options? = null) {
        val ids = state.value.selectedSaved.toList()
        if (ids.isEmpty() || state.value.exportProgress != null) return
        job = scope.launch {
            state.update { it.copy(exportProgress = ExportProgress(0, ids.size), exportSaved = null, exportNote = null, message = null, messageSaved = null) }
            var outcome = ExportOutcomeTally(0, ids.size, null)
            try {
                val items = container.savedImages.getByIdsChunked(ids).filter { readable(Uri.parse(it.uri)) }
                val progress = { done: Int -> state.update { it.copy(exportProgress = ExportProgress(done, ids.size)) } }
                outcome = withContext(Dispatchers.IO) {
                    when (kind) {
                        SavedExportKind.Zip -> toZip(items, ids.size, progress)
                        SavedExportKind.Folder -> toFolder(items, ids.size, tree, progress)
                        SavedExportKind.Pdf -> docs.pdf(items, ids.size, progress)
                        SavedExportKind.Contact -> docs.contact(items, ids.size, contact ?: ContactSheetLayout.Options(), progress)
                    }
                }
            } catch (e: CancellationException) {
                state.update { it.withMessage(FrameStrings.exportCancelled(), false, FrameIconsExtra.Cancel) }
                throw e
            } finally {
                state.update { it.copy(exportProgress = null) }
            }
            finishExport(outcome)
        }
    }

    private fun finishExport(outcome: ExportOutcomeTally) {
        val text = when {
            outcome.reason != null -> outcome.reason
            outcome.done == 0 -> FrameStrings.saveFailed()
            outcome.done == outcome.total -> SavedStrings.exportDone(outcome.done)
            else -> SavedStrings.exportPartial(outcome.done, outcome.total)
        }
        val failed = outcome.done < outcome.total
        val kept = outcome.zip.takeIf { outcome.done > 0 }
        state.update {
            it.copy(
                selectedSaved = if (outcome.done > 0) emptySet() else it.selectedSaved,
                exportSaved = kept,
                exportNote = if (kept != null && failed) text else null,
                message = if (kept == null) text else null,
                errorMessage = if (kept == null && failed) text else null,
                messageSaved = if (outcome.done > 0 && kept == null) SaveKind.File else null
            )
        }
    }

    private suspend fun saveOne(item: SavedImageEntity): GalleryStep {
        val uri = Uri.parse(item.uri)
        return when {
            readable(uri) && uri.authority == MediaStore.AUTHORITY -> GalleryStep.Already
            readable(uri) -> if (copyToGallery(item, uri)) GalleryStep.Saved else GalleryStep.Failed
            else -> if (resaveFromSource(item)) GalleryStep.Saved else GalleryStep.Failed
        }
    }

    private suspend fun resaveFromSource(item: SavedImageEntity): Boolean {
        val frame = item.sourceFrameId?.let { container.frames.getById(it) } ?: return false
        return runCatching { exporter.resaveFromFrame(item.id, frame) }.onFailure { logFailure("SavedActions", it) }.getOrDefault(false)
    }

    private suspend fun copyToGallery(item: SavedImageEntity, source: Uri): Boolean {
        val mime = withContext(Dispatchers.IO) { runCatching { resolver.getType(source) }.getOrNull() } ?: "image/jpeg"
        val root = when {
            mime.startsWith("image/") -> "Pictures"
            mime.startsWith("video/") -> "Movies"
            else -> "Documents"
        }
        val target = saver.save(item.displayName, mime, "$root/${FrameStrings.appName()}", null) { out -> copyTo(source, out) }
        return target != null
    }

    private fun copyTo(source: Uri, out: OutputStream) {
        resolver.openInputStream(source)?.use { it.copyTo(out) } ?: error("Source unavailable")
    }

    private fun readable(uri: Uri): Boolean =
        runCatching { resolver.openFileDescriptor(uri, "r")?.use { true } ?: false }.getOrDefault(false)

    private suspend fun toZip(items: List<SavedImageEntity>, total: Int, progress: (Int) -> Unit): ExportOutcomeTally {
        if (items.isEmpty()) return ExportOutcomeTally(0, total, null)
        val name = "${FrameStrings.appName()}_${System.currentTimeMillis()}.zip"
        var written = 0
        val uri = saver.save(name, "application/zip", "Documents/${FrameStrings.appName()}", container.prefs.saveFolderUri.value.ifBlank { null }) { out ->
            written = writeZip(items, out, progress)
        }
        return ExportOutcomeTally(if (uri == null) 0 else written, total, uri?.let { SavedCapture(it, "application/zip") })
    }

    private fun writeZip(items: List<SavedImageEntity>, out: OutputStream, progress: (Int) -> Unit): Int {
        val used = HashSet<String>()
        var count = 0
        ZipOutputStream(out.buffered()).use { zip ->
            zip.setLevel(1)
            items.forEach { item ->
                val ok = runCatching {
                    val input = resolver.openInputStream(Uri.parse(item.uri)) ?: error("Source unavailable")
                    input.use {
                        zip.putNextEntry(ZipEntry(unique(item.displayName, used)))
                        it.copyTo(zip)
                        zip.closeEntry()
                    }
                }.onFailure { logFailure("SavedActions", it) }.isSuccess
                if (ok) count++
                progress(count)
            }
        }
        return count
    }

    private fun toFolder(items: List<SavedImageEntity>, total: Int, tree: Uri?, progress: (Int) -> Unit): ExportOutcomeTally {
        if (tree == null) return ExportOutcomeTally(0, total, null)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        var count = 0
        items.forEach { item ->
            val source = Uri.parse(item.uri)
            val mime = runCatching { resolver.getType(source) }.getOrNull() ?: "image/jpeg"
            val target = runCatching { DocumentsContract.createDocument(resolver, parent, mime, item.displayName) ?: error("Create failed") }
                .onFailure { logFailure("SavedActions", it) }
                .getOrNull()
            val ok = target != null && runCatching {
                resolver.openOutputStream(target)?.use { copyTo(source, it) } ?: error("Output unavailable")
            }.onFailure {
                logFailure("SavedActions", it)
                runCatching { DocumentsContract.deleteDocument(resolver, target) }
            }.isSuccess
            if (ok) count++
            progress(count)
        }
        return ExportOutcomeTally(count, total, null)
    }

    private fun unique(name: String, used: MutableSet<String>): String {
        val stem = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        var candidate = name
        var n = 1
        while (!used.add(candidate.lowercase())) candidate = "${stem}_${n++}$ext"
        return candidate
    }
}

internal class ExportOutcomeTally(val done: Int, val total: Int, val zip: SavedCapture?, val reason: String? = null)

internal enum class GalleryStep { Saved, Already, Failed }

internal class GalleryTally {
    var saved = 0
    var already = 0
    var failed = 0

    fun add(step: GalleryStep) {
        when (step) {
            GalleryStep.Saved -> saved++
            GalleryStep.Already -> already++
            GalleryStep.Failed -> failed++
        }
    }

    fun message(): String = when {
        saved == 0 && failed == 0 -> SavedStrings.galleryAllThere()
        else -> listOfNotNull(
            SavedStrings.galleryDone(saved).takeIf { saved > 0 },
            SavedStrings.galleryThere(already).takeIf { already > 0 },
            SavedStrings.galleryFailed(failed).takeIf { failed > 0 }
        ).joinToString(FrameStrings.sep())
    }
}
