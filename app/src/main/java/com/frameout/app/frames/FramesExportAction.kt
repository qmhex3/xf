package com.frameout.app.frames

import com.frameout.core.storage.insertTracked
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.util.logged
import android.content.Context
import com.frameout.app.BuildConfig
import com.frameout.app.FrameOutApp
import com.frameout.app.collage.CollageMaterializer
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.SavedImageEntity
import com.frameout.core.export.ContactSheetImage
import com.frameout.core.export.ContactSheetLayout
import com.frameout.core.export.ContactSheetPdf
import com.frameout.core.export.EvidenceFrame
import com.frameout.core.export.EvidencePackage
import com.frameout.core.export.EvidenceSource
import com.frameout.core.export.FrameRow
import com.frameout.core.export.FramesTable
import com.frameout.core.export.FramesZip
import com.frameout.core.export.SlidesPdf
import com.frameout.core.storage.FrameExporter
import com.frameout.core.quality.SlideDeck
import com.frameout.core.util.QualityStrings
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec
import com.frameout.core.storage.FileSaver
import com.frameout.app.trim.SavedCapture
import com.frameout.core.util.FrameStrings
import com.frameout.engine.timeline.formatTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FramesExportAction(private val context: Context) {
    private val app = context.applicationContext as FrameOutApp
    private val saver = FileSaver(context)
    private val exporter = FrameExporter(context, app.container.storage, app.container.savedImages, app.container.frameEdits, app.container.projects, app.container.videoIndex)

    suspend fun gif(frames: List<FrameEntity>): ExportOutcome {
        if (frames.size < 2) return ExportOutcome(FrameStrings.gifNeedsTwo())
        val ordered = frames.sortedBy { it.ptsUs }
        val paths = materialize(ordered)
        if (!hasRoom(paths.size * GifSupport.FRAME_BYTES)) return ExportOutcome(FrameStrings.noSpace())
        val name = "${FrameStrings.appName()}_${System.currentTimeMillis()}.gif"
        val uri = saver.save(name, "image/gif", app.container.prefs.saveFolder.value, treeUri(), subfolderOf(ordered)) { out -> GifSupport.write(paths, GifSupport.delayMs(ordered), out) }
        val size = withContext(Dispatchers.IO) { GifSupport.dimensions(paths) }
        if (uri != null) app.container.savedImages.insertTracked(app.container.storage, SavedImageEntity(uri = uri.toString(), displayName = name, album = FrameStrings.appName(), width = size.first, height = size.second, createdAt = System.currentTimeMillis()))
        return result(uri, "image/gif")
    }

    suspend fun pdf(frames: List<FrameEntity>, title: String): ExportOutcome {
        if (frames.isEmpty()) return ExportOutcome(FrameStrings.nothingSaved())
        val ordered = frames.sortedBy { it.ptsUs }
        val paths = materialize(ordered)
        if (!hasRoom(paths.sumOf { path -> path?.let { File(it).length() } ?: 0L })) return ExportOutcome(FrameStrings.noSpace())
        val captions = ordered.map { formatTimestamp(it.ptsUs / 1000L) }
        val name = "${FrameStrings.appName()}_${System.currentTimeMillis()}.pdf"
        val uri = saver.save(name, "application/pdf", "Documents/${FrameStrings.appName()}", treeUri(), subfolderOf(ordered)) { out -> ContactSheetPdf.write(paths, captions, title, out) }
        return result(uri, "application/pdf")
    }

    suspend fun contactSheet(frames: List<FrameEntity>, title: String, options: ContactSheetLayout.Options): ExportOutcome {
        if (frames.isEmpty()) return ExportOutcome(FrameStrings.contactNeedsOne())
        val ordered = frames.sortedBy { it.ptsUs }
        val materializer = CollageMaterializer(context, app.container.projects, app.container.frameEdits)
        val aspect = withContext(Dispatchers.IO) { probeAspect(materializer, ordered) } ?: return ExportOutcome(FrameStrings.saveFailed())
        val plan = ContactSheetLayout.plan(ordered.size, aspect, options)
        if (plan.tooMany) return ExportOutcome(FrameStrings.contactTooMany())
        val pixels = plan.sheets.indices.sumOf { plan.sheetWidth.toLong() * plan.sheetHeight(it) }
        if (!hasRoom(pixels / if (options.png) 1 else 4)) return ExportOutcome(FrameStrings.noSpace())
        val job = SheetJob(plan, title, ordered, options, materializer, System.currentTimeMillis(), subfolderOf(ordered))
        var last: android.net.Uri? = null
        for (sheet in plan.sheets.indices) last = saveSheet(job, sheet) ?: break
        val mime = if (options.png) "image/png" else "image/jpeg"
        if (last == null || !job.complete) {
            com.frameout.core.rating.Rating.failure()
            return ExportOutcome(FrameStrings.saveFailed())
        }
        return result(last, mime)
    }

    private class SheetJob(
        val plan: ContactSheetLayout.Plan,
        val title: String,
        val frames: List<FrameEntity>,
        val options: ContactSheetLayout.Options,
        val materializer: CollageMaterializer,
        val stamp: Long,
        val subfolder: String
    ) {
        val times = frames.map { formatTimestamp(it.ptsUs / 1000L) }
        var saved = 0
        val complete: Boolean get() = saved == plan.sheets.size
    }

    private suspend fun renderSheet(job: SheetJob, sheet: Int): android.graphics.Bitmap? {
        var decoded = 0
        val side = maxOf(job.plan.cellW, job.plan.cellH)
        val bitmap = try {
            withContext(Dispatchers.IO) {
                ContactSheetImage.render(job.plan, sheet, job.title, job.times, job.options) { index ->
                    job.materializer.rendered(job.frames[index], side)?.also { decoded++ }
                }
            }
        } catch (_: OutOfMemoryError) {
            null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            com.frameout.core.util.logFailure("FramesExportAction", e)
            null
        }
        if (bitmap != null && decoded == 0) {
            bitmap.recycle()
            return null
        }
        return bitmap
    }

    private suspend fun saveSheet(job: SheetJob, sheet: Int): android.net.Uri? {
        val bitmap = renderSheet(job, sheet) ?: return null
        try {
            val png = job.options.png
            val name = ContactSheetLayout.fileName(FrameStrings.appName(), job.stamp, sheet, job.plan.sheets.size, png)
            val uri = saver.save(name, if (png) "image/png" else "image/jpeg", app.container.prefs.saveFolder.value, treeUri(), job.subfolder) { out -> ContactSheetImage.write(bitmap, png, out) } ?: return null
            job.saved++
            app.container.savedImages.insertTracked(app.container.storage, SavedImageEntity(uri = uri.toString(), displayName = name, album = FrameStrings.appName(), width = bitmap.width, height = bitmap.height, createdAt = System.currentTimeMillis()))
            return uri
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun probeAspect(materializer: CollageMaterializer, frames: List<FrameEntity>): Float? {
        for (frame in frames.take(PROBE_FRAMES)) {
            val bitmap = materializer.rendered(frame, PROBE_SIDE) ?: continue
            val aspect = bitmap.width.toFloat() / bitmap.height
            bitmap.recycle()
            return aspect
        }
        return null
    }

    suspend fun zip(frames: List<FrameEntity>, title: String): ExportOutcome {
        if (frames.isEmpty()) return ExportOutcome(FrameStrings.nothingSaved())
        val ordered = frames.sortedBy { it.ptsUs }
        val staged = runCatching { exporter.stageForArchive(ordered) }.logged("FramesExportAction").getOrDefault(emptyList())
        try {
            if (staged.isEmpty()) return ExportOutcome(FrameStrings.saveFailed())
            if (!hasRoom(staged.sumOf { it.file.length() })) return ExportOutcome(FrameStrings.noSpace())
            val name = "${FrameStrings.appName()}_${System.currentTimeMillis()}.zip"
            val uri = saver.save(name, "application/zip", "Documents/${FrameStrings.appName()}", treeUri(), subfolderOf(ordered)) { out -> FramesZip.write(staged, title, out) }
            return result(uri, "application/zip")
        } finally {
            exporter.clearArchiveStage(staged)
        }
    }

    suspend fun video(project: com.frameout.core.db.ProjectEntity, frames: List<FrameEntity>): ExportOutcome {
        if (frames.isEmpty()) return ExportOutcome(FrameStrings.nothingSaved())
        val sourceReadable = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openFileDescriptor(android.net.Uri.parse(project.sourceUri), "r")?.use { true } ?: false }.getOrDefault(false)
        }
        if (!sourceReadable) return ExportOutcome(FrameStrings.exportSourceMissing())
        val result = try {
            FrameReassembler.reassemble(context.applicationContext as android.app.Application, project, frames)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            com.frameout.core.util.logFailure("FramesExportAction", e)
            null
        }
        val done = result as? com.frameout.engine.edit.ReassembleResult.Success ?: return ExportOutcome(FrameStrings.reassembleFailed())
        val file = File(done.path)
        try {
            if (!hasRoom(file.length())) return ExportOutcome(FrameStrings.noSpace())
            val name = "${FrameStrings.appName()}_${System.currentTimeMillis()}.mp4"
            val uri = saver.save(name, "video/mp4", "Movies/${FrameStrings.appName()}", treeUri(), project.saveSubfolder) { out -> file.inputStream().use { it.copyTo(out) } }
            return result(uri, "video/mp4")
        } finally {
            file.delete()
        }
    }

    suspend fun csv(frames: List<FrameEntity>): ExportOutcome {
        if (frames.isEmpty()) return ExportOutcome(FrameStrings.nothingSaved())
        val ordered = frames.sortedBy { it.ptsUs }
        val source = app.container.projects.getById(ordered.first().projectId)?.sourceName.orEmpty()
        val rows = ordered.mapIndexed { i, f -> FrameRow(i + 1, f.index, f.id, f.ptsUs, f.isKeyframe, f.isFavorite, source) }
        val bytes = FramesTable.csv(rows).toByteArray(Charsets.UTF_8)
        val name = "${FrameStrings.appName()}_${System.currentTimeMillis()}.csv"
        val uri = saver.save(name, FramesTable.MIME, "Documents/${FrameStrings.appName()}", treeUri(), subfolderOf(ordered)) { out -> out.write(bytes) }
        return result(uri, FramesTable.MIME)
    }

    suspend fun evidence(frames: List<FrameEntity>, title: String): ExportOutcome {
        if (frames.isEmpty()) return ExportOutcome(FrameStrings.nothingSaved())
        val ordered = frames.sortedBy { it.ptsUs }
        val staged = runCatching { exporter.stageForArchive(ordered) }.logged("FramesExportAction").getOrDefault(emptyList())
        try {
            if (staged.isEmpty()) return ExportOutcome(FrameStrings.saveFailed())
            if (!hasRoom(staged.sumOf { it.file.length() })) return ExportOutcome(FrameStrings.noSpace())
            val hashed = withContext(Dispatchers.IO) { staged.map { EvidenceFrame(it, EvidencePackage.sha256(it.file)) } }
            val source = sourceEvidence(ordered.first())
            val createdAt = System.currentTimeMillis()
            val name = "${FrameStrings.appName()}_evidence_$createdAt.zip"
            val uri = saver.save(name, "application/zip", "Documents/${FrameStrings.appName()}", treeUri(), subfolderOf(ordered)) { out -> EvidencePackage.write(hashed, title, source, createdAt, BuildConfig.VERSION_NAME, out) }
            return result(uri, "application/zip")
        } finally {
            exporter.clearArchiveStage(staged)
        }
    }

    private suspend fun sourceEvidence(frame: FrameEntity): EvidenceSource = withContext(Dispatchers.IO) {
        val project = app.container.projects.getById(frame.projectId)
        val name = project?.sourceName.orEmpty()
        val hashed = runCatching {
            context.contentResolver.openInputStream(android.net.Uri.parse(project?.sourceUri.orEmpty()))?.use { EvidencePackage.sha256(it) }
        }.logged("FramesExportAction").getOrNull()
        EvidenceSource(name, hashed?.first, hashed?.second ?: 0L)
    }

    suspend fun slidesPdf(projectId: Long): ExportOutcome {
        val project = app.container.projects.getById(projectId)
        val dropTransitions = project?.let { ExtractModeCodec.decode(it.extractMode) is ExtractMode.Slides } ?: false
        val ids = runCatching { SlideDeck.uniqueIds(app.container.frames, projectId, dropTransitions) }.logged("FramesExportAction").getOrDefault(emptyList())
        if (ids.isEmpty()) return ExportOutcome(QualityStrings.noSlides())
        val ordered = app.container.frames.getByIdsChunked(ids).sortedBy { it.ptsUs }
        val paths = materialize(ordered)
        if (paths.none { it != null }) return ExportOutcome(FrameStrings.saveFailed())
        if (!hasRoom(paths.sumOf { path -> path?.let { File(it).length() } ?: 0L })) return ExportOutcome(FrameStrings.noSpace())
        val name = "${FrameStrings.appName()}_slides_${System.currentTimeMillis()}.pdf"
        val uri = saver.save(name, "application/pdf", "Documents/${FrameStrings.appName()}", treeUri(), subfolderOf(ordered)) { out -> SlidesPdf.write(paths, out) }
        return result(uri, "application/pdf")
    }

    private suspend fun materialize(frames: List<FrameEntity>): List<String?> =
        CollageMaterializer(context, app.container.projects, app.container.frameEdits).materialize(frames)

    private suspend fun subfolderOf(frames: List<FrameEntity>): String =
        frames.firstOrNull()?.let { app.container.projects.getById(it.projectId)?.saveSubfolder }.orEmpty()

    private fun hasRoom(bytes: Long): Boolean = app.container.storage.hasDestinationRoom(treeUri(), bytes)

    private fun treeUri(): String? = app.container.prefs.saveFolderUri.value.ifBlank { null }

    private fun result(uri: android.net.Uri?, mime: String): ExportOutcome {
        if (uri != null) com.frameout.core.rating.Rating.success() else com.frameout.core.rating.Rating.failure()
        return if (uri != null) ExportOutcome(FrameStrings.savedOk(), SavedCapture(uri, mime)) else ExportOutcome(FrameStrings.saveFailed())
    }

    private companion object {
        const val PROBE_FRAMES = 5
        const val PROBE_SIDE = 256
    }
}
