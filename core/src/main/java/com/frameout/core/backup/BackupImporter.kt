package com.frameout.core.backup

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room3.withWriteTransaction
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectEntity
import com.frameout.core.model.ProjectStatus
import com.frameout.core.storage.ImageFormat
import com.frameout.core.util.AppContainer
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class BackupImporter(private val context: Context, private val container: AppContainer) {
    suspend fun import(uri: Uri): BackupResult {
        val key = uri.toString()
        if (!IN_FLIGHT.add(key)) return BackupResult.Duplicate
        return try {
            LOCK.withLock { importOnce(uri) }
        } finally {
            IN_FLIGHT.remove(key)
        }
    }

    private suspend fun importOnce(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        var projectId = -1L
        val stage = File(container.storage.appProjectsDir(), "$STAGE_PREFIX${System.nanoTime()}")
        try {
            readInto(uri, stage) { projectId = it }
        } catch (e: CancellationException) {
            withContext(NonCancellable) { discard(projectId) }
            throw e
        } catch (e: BackupException) {
            fail(projectId, e, if (e.reason == BackupException.Reason.Newer) BackupResult.Newer else if (e.reason == BackupException.Reason.NoSpace) BackupResult.NoSpace else BackupResult.Invalid)
        } catch (e: OutOfMemoryError) {
            fail(projectId, e, BackupResult.Invalid)
        } catch (e: Exception) {
            fail(projectId, e, BackupResult.Failed)
        } finally {
            stage.deleteRecursively()
        }
    }

    private suspend fun fail(projectId: Long, error: Throwable, result: BackupResult): BackupResult {
        withContext(NonCancellable) { discard(projectId) }
        logFailure("BackupImport", error)
        return result
    }

    private suspend fun readInto(uri: Uri, stage: File, onId: (Long) -> Unit): BackupResult {
        if (!hasRoom(uri)) return BackupResult.NoSpace
        val input = context.contentResolver.openInputStream(uri) ?: return BackupResult.Failed
        return input.use { stream ->
            BackupReader(stream).use { reader ->
                val data = unique(reader.manifest())
                val entity = projectOf(data)
                if (isKnown(entity)) return@use BackupResult.Duplicate
                if (!stage.mkdirs() && !stage.isDirectory) throw BackupException(BackupException.Reason.NoSpace)
                val valid = data.frames.mapTo(HashSet(data.frames.size * 2)) { it.index }
                val written = reader.thumbs { index ->
                    if (index !in valid) return@thumbs null
                    if (context.filesDir.usableSpace < RESERVE_BYTES) throw BackupException(BackupException.Reason.NoSpace)
                    File(stage, "t_$index.jpg")
                }
                commit(entity, data, written, stage, onId)
                BackupResult.Imported
            }
        }
    }

    private suspend fun isKnown(e: ProjectEntity): Boolean =
        container.projects.findImported(e.name, e.sourceName, e.createdAt, e.durationMs, e.frameCount, e.lastPtsUs) != null

    private fun unique(data: BackupData): BackupData {
        val seen = HashSet<Int>(data.frames.size * 2)
        val frames = data.frames.filter { seen.add(it.index) }
        return if (frames.size == data.frames.size) data else BackupData(data.project, frames)
    }

    private suspend fun commit(entity: ProjectEntity, data: BackupData, written: Set<Int>, stage: File, onId: (Long) -> Unit): Long =
        container.database.withWriteTransaction {
            val projectId = container.projects.insert(entity)
            onId(projectId)
            val dir = container.storage.thumbsDir(projectId)
            written.forEach { index ->
                val target = File(dir, "t_$index.jpg")
                if (!File(stage, "t_$index.jpg").renameTo(target)) throw BackupException(BackupException.Reason.NoSpace)
            }
            data.frames.chunked(CHUNK).forEach { chunk ->
                container.frames.insertAll(chunk.map { f -> entityOf(projectId, f, if (f.index in written) File(dir, "t_${f.index}.jpg").absolutePath else null) })
            }
            val ids = container.frames.idsByIndex(projectId).associate { it.frameIndex to it.id }
            data.frames.forEach { f ->
                val edit = f.edit ?: return@forEach
                val id = ids[f.index] ?: return@forEach
                container.frameEdits.upsert(edit.toEntity(id))
                container.frameEdits.insertHistory(edit.toHistory(id))
            }
            val first = data.frames.firstOrNull { it.index in written }
            container.projects.setThumbnail(projectId, first?.let { File(dir, "t_${it.index}.jpg").absolutePath }, System.currentTimeMillis())
            projectId
        }

    private fun entityOf(projectId: Long, f: BackupFrame, thumb: String?) =
        FrameEntity(projectId = projectId, index = f.index, ptsUs = f.ptsUs, isKeyframe = f.isKeyframe, isFavorite = f.isFavorite, isDeleted = f.isDeleted, thumbnailPath = thumb)

    private fun projectOf(data: BackupData): ProjectEntity {
        val p = data.project
        val now = System.currentTimeMillis()
        return ProjectEntity(
            name = cleanText(p.name).ifBlank { "Backup" }, sourceUri = safeSourceUri(p.sourceUri), sourceName = cleanText(p.sourceName), durationMs = p.durationMs.coerceAtLeast(0L),
            frameCount = data.frames.size, extractedCount = data.frames.size,
            lastPtsUs = data.frames.lastOrNull()?.ptsUs ?: 0L, status = ProjectStatus.Completed.name,
            createdAt = p.createdAt.coerceIn(0L, now), updatedAt = now, extractMode = safeMode(p.extractMode), autoSave = false,
            saveFormat = safeFormat(p.saveFormat), jpegQuality = p.jpegQuality.coerceIn(1, 100), startMs = p.startMs.coerceAtLeast(0L), endMs = p.endMs.coerceAtLeast(0L),
            saveSubfolder = safeFolder(p.saveSubfolder)
        )
    }

    private fun cleanText(value: String): String = com.frameout.core.util.NameSanitizer.clean(value, max = minOf(BackupFormat.MAX_TEXT_CHARS, com.frameout.core.util.NameSanitizer.MAX_NAME_CHARS))

    private fun safeSourceUri(value: String): String {
        if (value.length > MAX_URI_CHARS) return ""
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return ""
        if (uri.scheme != "content") return ""
        val authority = uri.authority.orEmpty()
        if (authority.isBlank() || authority.startsWith(context.packageName)) return ""
        return value
    }

    private fun safeMode(value: String): String = if (MODE_PATTERN.matches(value)) value else "EveryFrame"

    private fun safeFormat(value: String): String = ImageFormat.entries.firstOrNull { it.key == value }?.key ?: "jpg"

    private fun safeFolder(value: String): String =
        value.replace(UNSAFE_FOLDER, "_").trim().trim('.').take(BackupFormat.MAX_TEXT_CHARS)

    private suspend fun discard(projectId: Long) {
        if (projectId < 0) return
        container.projects.delete(projectId)
        container.storage.deleteProject(projectId)
    }

    private fun hasRoom(uri: Uri): Boolean {
        val size = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { if (it.moveToFirst()) it.getLong(0) else 0L }
        }.getOrNull() ?: 0L
        return context.filesDir.usableSpace > size * 2 + RESERVE_BYTES
    }

    private companion object {
        val LOCK = Mutex()
        val IN_FLIGHT: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
        const val CHUNK = 500
        const val STAGE_PREFIX = ".import_"
        const val RESERVE_BYTES = 64L * 1024 * 1024
        const val MAX_URI_CHARS = 2048
        val MODE_PATTERN = Regex("EveryFrame|KeyframesOnly|SceneChange|Slides|(EveryNFrames|EveryNMillis|FixedCount):\\d{1,9}|Content:\\d{1,9}:[0-2]:\\d{1,9}:[01]:[a-z_,]{0,1200}")
        val UNSAFE_FOLDER = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    }
}
