package com.frameout.app.frames

import android.content.Context
import com.frameout.app.FrameOutApp
import com.frameout.app.trim.SavedCapture
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.export.DatasetArchive
import com.frameout.core.export.DatasetFormat
import com.frameout.core.export.DatasetFrame
import com.frameout.core.export.DatasetImages
import com.frameout.core.quality.DatasetOptions
import com.frameout.core.quality.DatasetPick
import com.frameout.core.quality.DatasetSelector
import com.frameout.core.quality.FrameSharpnessAnalyzer
import com.frameout.core.rating.Rating
import com.frameout.core.storage.FileSaver
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.DatasetStrings
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.core.util.logged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatasetExportAction(private val context: Context) {
    private val app = context.applicationContext as FrameOutApp
    private val saver = FileSaver(context)
    private val exporter = FrameExporter(context, app.container.storage, app.container.savedImages, app.container.frameEdits, app.container.projects, app.container.videoIndex)

    suspend fun export(projectId: Long, title: String, format: DatasetFormat, options: DatasetOptions): ExportOutcome {
        val scored = runCatching { FrameSharpnessAnalyzer.scoreProject(app.container.frames, projectId) }.logged("DatasetExport").getOrDefault(emptyList())
        if (scored.isEmpty()) return ExportOutcome(QualityStrings.noThumbnails())
        val result = DatasetSelector.split(scored, options)
        if (result.kept.isEmpty()) return ExportOutcome(DatasetStrings.none())
        val picks = result.kept + if (options.keepRejected) result.rejected else emptyList()
        val byId = app.container.frames.getByIdsChunked(picks.map { it.id }).associateBy { it.id }
        val chosen = picks.filter { it.id in byId }
        val keptCount = chosen.count { it in result.kept }
        val staged = runCatching { exporter.stageForArchive(chosen.map { byId.getValue(it.id) }) }.logged("DatasetExport").getOrDefault(emptyList())
        try {
            if (staged.size != chosen.size) return ExportOutcome(FrameStrings.saveFailed())
            return save(projectId, title, format, withContext(Dispatchers.IO) { frames(staged, chosen, keptCount) }, keptCount)
        } finally {
            exporter.clearArchiveStage(staged)
        }
    }

    private fun frames(staged: List<com.frameout.core.export.StagedFrame>, picks: List<DatasetPick>, keptCount: Int): List<DatasetFrame> =
        staged.mapIndexed { i, frame -> DatasetFrame(DatasetImages.usable(frame), picks[i].sharpness, picks[i].score, rejected = i >= keptCount) }

    private suspend fun save(projectId: Long, title: String, format: DatasetFormat, frames: List<DatasetFrame>, keptCount: Int): ExportOutcome {
        val treeUri = app.container.prefs.documentsTreeUri()
        if (!app.container.storage.hasDestinationRoom(treeUri, frames.sumOf { it.staged.file.length() })) return ExportOutcome(FrameStrings.noSpace())
        val subfolder = app.container.projects.getById(projectId)?.let { com.frameout.core.storage.AsciiName.folder(it.saveSubfolder, it.id) }.orEmpty()
        val now = System.currentTimeMillis()
        val name = "${FrameStrings.appName()}_${format.key}_$now.zip"
        val uri = saver.save(name, "application/zip", "Documents/${FrameStrings.appName()}", treeUri, subfolder) { out -> DatasetArchive.write(format, frames, title, now, out) }
        if (uri == null) {
            Rating.failure()
            return ExportOutcome(FrameStrings.saveFailed())
        }
        Rating.success()
        return ExportOutcome(DatasetStrings.saved(keptCount), SavedCapture(uri, "application/zip"))
    }
}
