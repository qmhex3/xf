package com.frameout.core.storage

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.frameout.core.db.FrameEditDao
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectDao
import com.frameout.core.db.VideoIndexDao
import com.frameout.core.db.SavedImageDao
import com.frameout.core.db.SavedImageEntity
import com.frameout.core.edit.FrameEdit
import com.frameout.core.edit.toEdit
import com.frameout.core.export.StagedFrame
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.edit.GainmapOps
import com.frameout.core.util.FrameStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FrameExporter(private val context: Context, private val storage: StorageManager, private val savedImageDao: SavedImageDao, private val frameEditDao: FrameEditDao, private val projectDao: ProjectDao, private val videoIndexDao: VideoIndexDao? = null) {
    companion object {
        const val MAX_SHARE_FRAMES = 30
    }

    private val prefs = AppPrefs(context)
    private val loader = ExportSourceLoader(context, projectDao, videoIndexDao)
    private val exif = ExifWriter(context)
    private val meta = ExportMetadata(context, projectDao, prefs)

    @Volatile
    var lastMetadataFailures: Int = 0
        private set

    private val stageDirs = java.util.Collections.synchronizedSet(HashSet<File>())

    @Volatile
    var lastSavedIds: List<Long> = emptyList()
        private set

    suspend fun saveToGallery(frames: List<FrameEntity>, onProgress: (Int) -> Unit = {}): Int = withContext(Dispatchers.IO) {
        val ids = ArrayList<Long>()
        lastMetadataFailures = 0
        try {
            frames.forEachIndexed { position, frame ->
                ensureActive()
                saveOne(frame, position + 1, frames.size, ids)
                onProgress(position + 1)
            }
        } finally {
            loader.release()
            lastSavedIds = ids
        }
        ids.size
    }

    suspend fun stageForArchive(frames: List<FrameEntity>): List<StagedFrame> = withContext(Dispatchers.IO) {
        val root = File(context.cacheDir, "frameout_zip")
        val dir = File(root, "stage_${System.nanoTime()}")
        check(dir.mkdirs() || dir.isDirectory) { "Unable to create archive staging directory" }
        stageDirs += dir
        val used = HashSet<String>()
        val staged = ArrayList<StagedFrame>()
        var stagedBytes = 0L
        var complete = false
        try {
            frames.forEachIndexed { position, frame ->
                checkArchiveSpace(frames.size, staged.size, stagedBytes)
                stagedBytes += stageArchiveFrame(frame, position, frames.size, dir, used, staged)
            }
            complete = true
            staged
        } finally {
            loader.release()
            if (!complete) {
                dir.deleteRecursively()
                stageDirs -= dir
            }
        }
    }

    private fun checkArchiveSpace(total: Int, stagedCount: Int, stagedBytes: Long) {
        if (stagedCount == 0) {
            check(cacheAvailableBytes() >= ARCHIVE_HEADROOM_BYTES) { "Not enough space to stage archive" }
            return
        }
        val perFrame = stagedBytes / stagedCount
        val remaining = total - stagedCount
        val required = perFrame * remaining + perFrame * total + ARCHIVE_HEADROOM_BYTES
        check(cacheAvailableBytes() >= required) { "Not enough space to stage archive" }
    }

    private suspend fun stageArchiveFrame(
        frame: FrameEntity,
        position: Int,
        total: Int,
        dir: File,
        used: MutableSet<String>,
        staged: MutableList<StagedFrame>
    ): Long {
        val result = renderOne(frame, position + 1, total)
            ?: throw java.io.IOException("Unable to render frame ${frame.id} for archive")
        try {
            val name = FrameNaming.unique(result.name, used)
            val file = File(dir, "${staged.size}_${frame.id}.${result.format.ext}")
            FileOutputStream(file).use {
                check(result.format.compress(result.bitmap, prefs.jpegQuality.value, it, meta.exifBlock(result.format, result.createdAt, frame)))
            }
            val stamp = meta.stampFor(result.createdAt, frame)
            if (!exif.embeddedAtEncode(result.format) && exif.supports(result.format, stamp) && !exif.writeFile(file, result.format, stamp)) {
                throw java.io.IOException("Unable to write image metadata")
            }
            staged += StagedFrame(name, file, frame.id, frame.index, position + 1, frame.ptsUs, result.width, result.height)
            return file.length()
        } finally {
            if (!result.bitmap.isRecycled) result.bitmap.recycle()
        }
    }

    fun clearArchiveStage() {
        val owned = synchronized(stageDirs) { stageDirs.toList().also { stageDirs.clear() } }
        owned.forEach { it.deleteRecursively() }
    }

    fun clearArchiveStage(staged: List<StagedFrame>) {
        staged.mapNotNull { it.file.parentFile }.toSet().forEach {
            it.deleteRecursively()
            stageDirs.remove(it)
        }
    }

    private fun cacheAvailableBytes(): Long = runCatching {
        val stat = android.os.StatFs(context.cacheDir.absolutePath)
        stat.availableBlocksLong * stat.blockSizeLong
    }.getOrDefault(Long.MAX_VALUE)

    suspend fun resaveFromFrame(savedId: Long, frame: FrameEntity): Boolean = withContext(Dispatchers.IO) {
        val old = savedImageDao.getById(savedId) ?: return@withContext false
        val rendered = try { renderOne(frame) } finally { loader.release() }
        val result = rendered ?: return@withContext false
        val newUri = try {
            storage.saveBitmapToGallery(result.bitmap, result.name, result.format, prefs.jpegQuality.value, prefs.saveFolder.value, result.createdAt, prefs.saveFolderUri.value.ifBlank { null }, meta.subfolderOf(frame), meta.exifBlock(result.format, result.createdAt, frame))
        } finally {
            if (!result.bitmap.isRecycled) result.bitmap.recycle()
        }
        if (newUri == null) return@withContext false
        if (!writeExif(newUri, result, frame)) {
            storage.deleteGalleryUri(newUri.toString())
            return@withContext false
        }
        try {
            savedImageDao.updateUri(savedId, newUri.toString(), result.name, result.width, result.height, result.createdAt)
        } catch (e: Exception) {
            storage.deleteGalleryUri(newUri.toString())
            throw e
        }
        if (!storage.deleteGalleryUri(old.uri)) {
            val rolledBack = runCatching {
                savedImageDao.updateUri(savedId, old.uri, old.displayName, old.width, old.height, old.createdAt)
                true
            }.getOrDefault(false)
            if (rolledBack) {
                storage.deleteGalleryUri(newUri.toString())
                return@withContext false
            }
            com.frameout.core.util.DiagLog.append("export", "resave kept new image because old image could not be deleted for savedId=$savedId")
            return@withContext true
        }
        true
    }

    suspend fun shareIntent(frames: List<FrameEntity>, includeLocation: Boolean = true): Intent? = withContext(Dispatchers.IO) {
        storage.cleanupShareCache()
        val limited = frames.take(MAX_SHARE_FRAMES)
        val uris = ArrayList<Uri>()
        var mime: String? = null
        try {
            limited.forEach { frame ->
                val result = renderOne(frame) ?: return@forEach
                val output = result.format
                val file = File(context.cacheDir, "frameout_share/frame_${frame.id}.${output.ext}")
                val stamp = meta.stampFor(result.createdAt, frame, includeLocation)
                try {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { check(output.compress(result.bitmap, prefs.jpegQuality.value, it, meta.exifBlock(output, result.createdAt, frame, includeLocation))) }
                } finally {
                    if (!result.bitmap.isRecycled) result.bitmap.recycle()
                }
                mime = if (mime == null || mime == output.mime) output.mime else "image/*"
                if (!exif.embeddedAtEncode(output) && exif.supports(output, stamp) && !exif.writeFile(file, output, stamp)) {
                    lastMetadataFailures++
                    com.frameout.core.util.DiagLog.append("export", "metadata not written for shared frame ${frame.id}")
                }
                uris += FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } finally {
            loader.release()
        }
        when (uris.size) {
            0 -> null
            1 -> Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uris.first()); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            else -> Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = mime; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    private suspend fun saveOne(frame: FrameEntity, sequence: Int, total: Int, ids: MutableList<Long>): Uri? {
        val result = renderOne(frame, sequence, total) ?: return null
        val uri = try {
            storage.saveBitmapToGallery(result.bitmap, result.name, result.format, prefs.jpegQuality.value, prefs.saveFolder.value, result.createdAt, prefs.saveFolderUri.value.ifBlank { null }, meta.subfolderOf(frame), meta.exifBlock(result.format, result.createdAt, frame))
        } finally {
            if (!result.bitmap.isRecycled) result.bitmap.recycle()
        }
        if (uri == null) return null
        try {
            writeExif(uri, result, frame)
            ids += savedImageDao.insert(SavedImageEntity(uri = uri.toString(), displayName = result.name, album = FrameStrings.appName(), width = result.width, height = result.height, createdAt = result.createdAt, sourceFrameId = frame.id))
        } catch (e: Exception) {
            storage.deleteGalleryUri(uri.toString())
            throw e
        }
        return uri
    }

    private suspend fun writeExif(uri: Uri, result: Rendered, frame: FrameEntity): Boolean {
        if (exif.embeddedAtEncode(result.format)) return true
        val stamp = meta.stampFor(result.createdAt, frame)
        if (!exif.supports(result.format, stamp)) return true
        val ok = exif.write(uri, result.format, stamp)
        if (!ok) {
            lastMetadataFailures++
            com.frameout.core.util.DiagLog.append("export", "metadata not written for frame ${frame.id}")
        }
        return ok
    }


    private suspend fun renderOne(frame: FrameEntity, sequence: Int = frame.index + 1, total: Int = 0): Rendered? {
        val source = sourceBitmap(frame) ?: return null
        val edit = frameEditDao.get(frame.id)?.toEdit() ?: FrameEdit(frame.id)
        val edited = applyFrameEdit(source, edit)
        if (edited !== source) source.recycle()
        val bitmap = CaptureSize.apply(edited, prefs.captureSize.value)
        if (bitmap !== edited) edited.recycle()
        val format = outputFormat(bitmap)
        val createdAt = meta.createdAt(frame)
        val projectName = projectDao.getById(frame.projectId)?.name.orEmpty()
        val name = FrameNaming.render(prefs.namingTemplate.value, projectName, frame.projectId, frame.index, frame.ptsUs, sequence, total, format.ext)
        return Rendered(bitmap, name, format, bitmap.width, bitmap.height, createdAt)
    }

    private data class Rendered(val bitmap: Bitmap, val name: String, val format: ImageFormat, val width: Int, val height: Int, val createdAt: Long)
    private suspend fun sourceBitmap(frame: FrameEntity): Bitmap? = loader.load(frame)
    private fun outputFormat(bitmap: Bitmap): ImageFormat = ImageFormat.resolve(prefs.saveFormat.value, GainmapOps.has(bitmap), hasSignal = com.frameout.core.hdr.HdrSignalRegistry.has(bitmap))
    private fun applyFrameEdit(source: Bitmap, edit: FrameEdit): Bitmap = com.frameout.core.edit.BitmapEditRenderer.render(source, edit)
}

private const val ARCHIVE_HEADROOM_BYTES = 64L * 1024L * 1024L
