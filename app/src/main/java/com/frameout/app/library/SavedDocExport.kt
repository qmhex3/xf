package com.frameout.app.library

import com.frameout.core.storage.insertTracked
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import com.frameout.app.trim.SavedCapture
import com.frameout.core.db.SavedImageEntity
import com.frameout.core.export.ContactSheetImage
import com.frameout.core.export.ContactSheetLayout
import com.frameout.core.export.ContactSheetPdf
import com.frameout.core.storage.FileSaver
import com.frameout.core.util.AppContainer
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SavedStrings
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

internal class SavedDocExport(
    private val app: Application,
    private val container: AppContainer,
    private val saver: FileSaver
) {
    private val resolver get() = app.contentResolver
    private val media = SavedMediaDecoder(app)

    private class Staged(val file: File, val item: SavedImageEntity)

    private class Sheet(
        val plan: ContactSheetLayout.Plan,
        val items: List<SavedImageEntity>,
        val options: ContactSheetLayout.Options,
        val stamp: Long
    ) {
        var saved = 0
        var decodedTotal = 0
        var pendingDecoded = 0
    }

    suspend fun pdf(items: List<SavedImageEntity>, total: Int, progress: (Int) -> Unit): ExportOutcomeTally {
        val staged = stage(items.chronological(), progress)
        try {
            if (staged.isEmpty()) return fail(total, SavedStrings.exportNoneReadable())
            if (!hasDocRoom(maxOf(staged.sumOf { it.file.length() }, staged.size * PDF_CELL_BYTES))) return fail(total, FrameStrings.noSpace())
            val name = "${FrameStrings.appName()}_${System.currentTimeMillis()}.pdf"
            val paths = staged.map { it.file.absolutePath }
            val captions = staged.map { it.item.displayName }
            val title = FrameStrings.appName()
            val uri = saver.save(name, "application/pdf", "Documents/${FrameStrings.appName()}", docTreeUri()) { out -> ContactSheetPdf.write(paths, captions, title, out) }
            return outcome(uri, "application/pdf", staged.size, total)
        } finally {
            staged.forEach { it.file.delete() }
        }
    }

    suspend fun contact(unordered: List<SavedImageEntity>, total: Int, options: ContactSheetLayout.Options, progress: (Int) -> Unit): ExportOutcomeTally {
        val items = unordered.chronological()
        val chosen = options.copy(showTime = false)
        val aspect = withContext(Dispatchers.IO) { probeAspect(items) } ?: return fail(total, SavedStrings.exportNoneReadable())
        val plan = ContactSheetLayout.plan(items.size, aspect, chosen)
        if (plan.tooMany) return fail(total, FrameStrings.contactTooMany())
        val pixels = plan.sheets.indices.sumOf { plan.sheetWidth.toLong() * plan.sheetHeight(it) }
        if (!hasRoom(pixels / if (chosen.png) 1 else 4)) return fail(total, FrameStrings.noSpace())
        val job = Sheet(plan, items, chosen, System.currentTimeMillis())
        var last: Uri? = null
        for (sheet in plan.sheets.indices) {
            last = saveSheet(job, sheet) ?: break
            progress(job.items.size.coerceAtMost((sheet + 1) * plan.rowsPerSheet * plan.columns))
        }
        val mime = if (chosen.png) "image/png" else "image/jpeg"
        if (job.saved == 0) return fail(total, FrameStrings.saveFailed())
        return outcome(last, mime, job.decodedTotal, total)
    }

    private fun List<SavedImageEntity>.chronological() = sortedWith(compareBy({ it.createdAt }, { it.id }))

    private suspend fun saveSheet(job: Sheet, sheet: Int): Uri? {
        val bitmap = renderSheet(job, sheet) ?: return null
        try {
            val png = job.options.png
            val name = ContactSheetLayout.fileName(FrameStrings.appName(), job.stamp, sheet, job.plan.sheets.size, png)
            val mime = if (png) "image/png" else "image/jpeg"
            val uri = saver.save(name, mime, container.prefs.saveFolder.value, treeUri()) { out -> ContactSheetImage.write(bitmap, png, out) } ?: return null
            job.saved++
            job.decodedTotal += job.pendingDecoded
            container.savedImages.insertTracked(container.storage, SavedImageEntity(uri = uri.toString(), displayName = name, album = FrameStrings.appName(), width = bitmap.width, height = bitmap.height, createdAt = System.currentTimeMillis()))
            return uri
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun renderSheet(job: Sheet, sheet: Int): Bitmap? {
        var decoded = 0
        val side = maxOf(job.plan.cellW, job.plan.cellH)
        val times = job.items.map { "" }
        val bitmap = try {
            withContext(Dispatchers.IO) {
                ContactSheetImage.render(job.plan, sheet, FrameStrings.appName(), times, job.options) { index ->
                    job.items[index].let { media.decode(Uri.parse(it.uri), it.displayName, side) }?.also { decoded++ }
                }
            }
        } catch (_: OutOfMemoryError) {
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFailure("SavedDocExport", e)
            null
        }
        job.pendingDecoded = decoded
        if (bitmap != null && decoded == 0) {
            bitmap.recycle()
            return null
        }
        return bitmap
    }

    private suspend fun stage(items: List<SavedImageEntity>, progress: (Int) -> Unit): List<Staged> = withContext(Dispatchers.IO) {
        val dir = File(app.cacheDir, "saved_export").apply { mkdirs() }
        val out = ArrayList<Staged>()
        try {
            items.forEachIndexed { index, item ->
                ensureActive()
                val file = File(dir, "${System.nanoTime()}_$index")
                val ok = runCatching { prepare(item, file) }.onFailure { if (it is CancellationException) throw it; logFailure("SavedDocExport", it) }.getOrDefault(false)
                if (ok) out.add(Staged(file, item)) else file.delete()
                progress(out.size)
            }
        } catch (e: CancellationException) {
            out.forEach { it.file.delete() }
            throw e
        }
        out
    }

    private fun prepare(item: SavedImageEntity, target: File): Boolean {
        val uri = Uri.parse(item.uri)
        val room = target.parentFile?.usableSpace ?: 0L
        val size = runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: -1L
        val plainPhoto = !media.isMoving(uri, item.displayName) && media.orientation(uri) == ExifInterface.ORIENTATION_NORMAL
        if (plainPhoto && copyAndCheck(uri, size, room, target)) return true
        target.delete()
        if (room < CACHE_HEADROOM) return false
        return media.stageTo(uri, item.displayName, STAGE_SIDE, target)
    }

    private fun copyAndCheck(source: Uri, size: Long, room: Long, target: File): Boolean {
        if (size > 0 && size + CACHE_HEADROOM > room) return false
        val copied = runCatching {
            resolver.openInputStream(source)?.use { input -> target.outputStream().use { input.copyTo(it) } } != null
        }.getOrDefault(false)
        if (!copied) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(target.absolutePath, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    private fun probeAspect(items: List<SavedImageEntity>): Float? {
        for (item in items.take(PROBE_ITEMS)) {
            if (item.width > 0 && item.height > 0) return item.width.toFloat() / item.height
            val bounds = media.probe(Uri.parse(item.uri)) ?: continue
            return bounds.first.toFloat() / bounds.second
        }
        return null
    }

    private fun outcome(uri: Uri?, mime: String, done: Int, total: Int): ExportOutcomeTally {
        if (uri == null) return fail(total, FrameStrings.saveFailed())
        return ExportOutcomeTally(done, total, SavedCapture(uri, mime))
    }

    private fun fail(total: Int, reason: String) = ExportOutcomeTally(0, total, null, reason)

    private fun hasRoom(bytes: Long): Boolean = container.storage.hasDestinationRoom(treeUri(), bytes)

    private fun treeUri(): String? = container.prefs.saveFolderUri.value.ifBlank { null }

    private fun docTreeUri(): String? = container.prefs.documentsTreeUri()

    private fun hasDocRoom(bytes: Long): Boolean = container.storage.hasDestinationRoom(docTreeUri(), bytes)

    private companion object {
        const val PROBE_ITEMS = 5
        const val STAGE_SIDE = 1600
        const val PDF_CELL_BYTES = 1_500_000L
        const val CACHE_HEADROOM = 64L * 1024 * 1024
    }
}
