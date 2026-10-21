package com.frameout.core.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.frameout.core.db.ProjectEntity
import com.frameout.core.util.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BackupExporter(private val context: Context, private val container: AppContainer) {
    private class Loaded(val data: BackupData, val paths: Map<Int, String>)

    fun suggestedName(project: ProjectEntity): String = BackupFormat.fileName(project.name, System.currentTimeMillis())

    suspend fun exportTo(projectId: Long, target: Uri): BackupResult {
        val project = container.projects.getById(projectId) ?: return BackupResult.Failed
        return writeTo(load(project), target)
    }

    suspend fun exportToFolder(projectId: Long, folder: Uri): Pair<BackupResult, String?> {
        val project = container.projects.getById(projectId) ?: return BackupResult.Failed to null
        val name = suggestedName(project)
        val loaded = load(project)
        val created = withContext(Dispatchers.IO) {
            runCatching {
                val parent = DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
                DocumentsContract.createDocument(context.contentResolver, parent, MIME_GENERIC, name)
            }.getOrNull()
        } ?: return BackupResult.Failed to null
        return writeTo(loaded, created) to name
    }

    private suspend fun writeTo(loaded: Loaded, target: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(target, "wt")?.use { out -> write(loaded, out) } ?: return@withContext BackupResult.Failed
            BackupResult.Saved
        } catch (e: CancellationException) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, target) }
            throw e
        } catch (e: Exception) {
            com.frameout.core.util.logFailure("BackupExport", e)
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, target) }
            if (e.message?.contains("ENOSPC") == true) BackupResult.NoSpace else BackupResult.Failed
        }
    }

    private fun write(loaded: Loaded, out: java.io.OutputStream) {
        if (loaded.data.frames.size > BackupFormat.MAX_FRAMES) throw BackupException(BackupException.Reason.TooLarge)
        val writer = BackupWriter(out)
        writer.manifest(loaded.data)
        loaded.data.frames.forEach { frame -> loaded.paths[frame.index]?.let { writer.thumb(frame.index, File(it)) } }
        writer.finish()
    }

    private suspend fun load(project: ProjectEntity): Loaded = withContext(Dispatchers.IO) { loadBlocking(project) }

    private suspend fun loadBlocking(project: ProjectEntity): Loaded {
        val frames = ArrayList<BackupFrame>()
        val paths = HashMap<Int, String>()
        var after = Int.MIN_VALUE
        while (true) {
            val page = container.frames.pageAllAfter(project.id, after, PAGE)
            if (page.isEmpty()) break
            val edits = container.frameEdits.getMany(page.map { it.id }).associateBy { it.frameId }
            page.forEach { frame ->
                frames.add(frame.toBackup(edits[frame.id]))
                frame.thumbnailPath?.let { paths[frame.index] = it }
            }
            after = page.last().index
        }
        return Loaded(BackupData(project.toBackup(), frames), paths)
    }

    private companion object {
        const val PAGE = 500
        const val MIME_GENERIC = "application/octet-stream"
        const val ROW_BYTES = 48L
    }
}
