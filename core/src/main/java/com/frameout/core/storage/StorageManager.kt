package com.frameout.core.storage

import com.frameout.core.util.logFailure
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager as AndroidStorageManager
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.media.MediaScannerConnection
import com.frameout.core.edit.GainmapOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val MAX_DELETE_REQUEST_URIS = 2000

class StorageManager(private val context: Context) {
    fun appProjectsDir(): File = File(context.filesDir, "projects").also { it.mkdirs() }
    fun projectDir(projectId: Long): File = File(appProjectsDir(), projectId.toString()).also { it.mkdirs() }
    fun thumbsDir(projectId: Long): File = File(projectDir(projectId), "thumbs").also { it.mkdirs() }
    fun availableBytes(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }
    fun hasHeadroom(): Boolean = availableBytes() > HEADROOM_BYTES
    fun hasSpaceForResolution(count: Int, width: Int, height: Int, autoSave: Boolean, format: String = "jpg"): Boolean {
        val known = width > 0 && height > 0
        val pixels = if (known) width.toLong() * height.toLong() else UNKNOWN_SIZE_PIXELS
        val fullFrame = if (autoSave) (pixels * bytesPerPixel(format)).toLong() else 0L
        val bytesPerFrame = fullFrame + THUMBNAIL_BYTES
        val frames = count.toLong().coerceAtLeast(1L).coerceAtMost(Long.MAX_VALUE / (2L * bytesPerFrame))
        return availableBytes() > frames * bytesPerFrame + SPACE_MARGIN_BYTES
    }

    private fun bytesPerPixel(format: String): Double = if (format.equals("png", ignoreCase = true)) 3.0 else 0.6
    fun deleteProject(projectId: Long): Boolean = runCatching {
        val dir = File(appProjectsDir(), projectId.toString())
        !dir.exists() || dir.deleteRecursively()
    }.getOrDefault(false)
    fun cleanupShareCache(maxAgeMs: Long = 24L * 60L * 60L * 1000L, maxFiles: Int = 100) {
        val dir = File(context.cacheDir, "frameout_share")
        val files = dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: return
        val cutoff = System.currentTimeMillis() - maxAgeMs
        files.forEachIndexed { index, file ->
            if (index >= maxFiles || file.lastModified() < cutoff) file.delete()
        }
    }
    fun cleanupArchiveStages(maxAgeMs: Long = 60L * 60L * 1000L) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        File(context.cacheDir, "frameout_zip").listFiles()?.forEach { if (it.lastModified() < cutoff) it.deleteRecursively() }
    }
    fun cleanupMotionCache(maxAgeMs: Long = 3L * 24L * 60L * 60L * 1000L, maxFiles: Int = 30, tempAgeMs: Long = 60L * 60L * 1000L) {
        val dir = File(context.cacheDir, "motion")
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        val now = System.currentTimeMillis()
        val (temps, videos) = files.partition { it.name.endsWith(".tmp") }
        temps.filter { it.lastModified() < now - tempAgeMs }.forEach { it.delete() }
        videos.sortedByDescending { it.lastModified() }.forEachIndexed { index, file ->
            if (index >= maxFiles || file.lastModified() < now - maxAgeMs) file.delete()
        }
    }
    fun cleanupTemporaryFiles(maxAgeMs: Long = 24L * 60L * 60L * 1000L) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        appProjectsDir().listFiles()?.forEach { project ->
            if (!project.isDirectory) return@forEach
            if (project.name.startsWith(".import_")) {
                if (project.lastModified() < cutoff) project.deleteRecursively()
                return@forEach
            }
            project.walkTopDown().filter { it.isFile && it.name.endsWith(".tmp") && it.lastModified() < cutoff }.forEach { it.delete() }
        }
    }
    suspend fun deleteGalleryUri(uri: String): Boolean = withContext(Dispatchers.IO) {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull()
        if (parsed == null || parsed.scheme.isNullOrBlank()) return@withContext true
        val deleted = runCatching { context.contentResolver.delete(parsed, null, null) > 0 }.getOrElse {
            logFailure("StorageManager", it)
            false
        }
        if (deleted) return@withContext true
        val documentDeleted = DocumentsContract.isDocumentUri(context, parsed) &&
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, parsed) }.getOrDefault(false)
        if (documentDeleted) return@withContext true
        when (galleryUriExists(parsed)) {
            false -> true
            true, null -> false
        }
    }

    fun galleryDeleteRequest(uris: List<String>): android.content.IntentSender? {
        if (Build.VERSION.SDK_INT < 29) return null
        val media = uris.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
            .filter { it.authority == MediaStore.AUTHORITY && galleryUriExists(it) != false }
            .take(MAX_DELETE_REQUEST_URIS)
        if (media.isEmpty()) return null
        if (Build.VERSION.SDK_INT >= 30) {
            return runCatching { MediaStore.createDeleteRequest(context.contentResolver, media).intentSender }.getOrNull()
        }
        for (uri in media) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (e: android.app.RecoverableSecurityException) {
                return e.userAction.actionIntent.intentSender
            } catch (e: Exception) {
                logFailure("StorageManager", e)
            }
        }
        return null
    }

    private fun galleryUriExists(uri: Uri): Boolean? = try {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (e: java.io.FileNotFoundException) {
        if (uri.authority == MediaStore.AUTHORITY && !canReadMedia()) null else false
    } catch (e: Exception) {
        null
    }

    fun canReadMedia(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_IMAGES else android.Manifest.permission.READ_EXTERNAL_STORAGE
        return androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun uriExists(uri: String): Boolean? {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return false
        if (parsed.scheme.isNullOrBlank()) return false
        return galleryUriExists(parsed)
    }

    fun cleanupUndoCache() = GalleryBackup(context).cleanup()
    suspend fun saveBitmapToGallery(bitmap: Bitmap, displayName: String, format: ImageFormat = ImageFormat.Jpg, quality: Int = 92, relativePath: String = "Pictures/FrameOut", dateTaken: Long? = null, requestedTree: String? = null, subfolder: String = "", exif: ByteArray? = null): Uri? = withContext(Dispatchers.IO) {
        val treeUri = SaveFolderGuard.resolve(context, requestedTree)
        val required = bitmap.byteCount.toLong().coerceAtLeast(1L) + HEADROOM_BYTES
        val available = destinationAvailableBytes(treeUri)
        if (available <= HEADROOM_BYTES || available <= required) return@withContext null
        if (!treeUri.isNullOrBlank()) {
            saveToTree(bitmap, displayName, format, quality, Uri.parse(treeUri), subfolder, exif)?.let { return@withContext it }
            SaveFolderGuard.fellBack()
        }
        val cleanPath = SaveLocation.join(relativePath, subfolder)
        val legacyFile = if (Build.VERSION.SDK_INT < 29) File(Environment.getExternalStorageDirectory(), "$cleanPath/$displayName") else null
        legacyFile?.parentFile?.mkdirs()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            dateTaken?.let { put(MediaStore.Images.Media.DATE_TAKEN, it) }
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "$cleanPath/")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.IS_PENDING, 1)
            legacyFile?.let { put(MediaStore.Images.Media.DATA, it.absolutePath) }
        }
        val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return@withContext null
        try {
            resolver.openOutputStream(uri)?.use { out ->
                check(format.compress(bitmap, quality, out, exif))
            } ?: error("Output unavailable")
            if (Build.VERSION.SDK_INT >= 29) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            } else legacyFile?.let { MediaScannerConnection.scanFile(context, arrayOf(it.absolutePath), arrayOf(format.mime), null) }
            uri
        } catch (e: Exception) {
            logFailure("StorageManager", e); resolver.delete(uri, null, null); null
        }
    }
    fun hasDestinationRoom(treeUri: String?, bytes: Long): Boolean = destinationAvailableBytes(treeUri) > bytes + HEADROOM_BYTES
    private fun destinationAvailableBytes(treeUri: String?): Long {
        val tree = treeUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        val treeVolume = tree?.let(::treeVolumeAvailableBytes)
        if (treeVolume != null) return treeVolume
        val primaryPath = Environment.getExternalStorageDirectory().absolutePath
        return runCatching { StatFs(primaryPath).availableBytes }.getOrElse { availableBytes() }
    }

    private fun treeVolumeAvailableBytes(tree: Uri): Long? {
        if (Build.VERSION.SDK_INT < 30) return null
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: return null
        val volumeId = documentId.substringBefore(':')
        val storage = context.getSystemService(Context.STORAGE_SERVICE) as? AndroidStorageManager ?: return null
        val volume = storage.storageVolumes.firstOrNull { volumeId.equals("primary", ignoreCase = true) && it.isPrimary || it.uuid == volumeId } ?: return null
        val directory = volume.directory ?: return null
        return runCatching { StatFs(directory.absolutePath).availableBytes }.getOrNull()
    }

    private fun saveToTree(bitmap: Bitmap, name: String, format: ImageFormat, quality: Int, tree: Uri, subfolder: String, exif: ByteArray?): Uri? = runCatching {
        val parent = SaveLocation.treeParent(context.contentResolver, tree, subfolder)
        val uri = DocumentsContract.createDocument(context.contentResolver, parent, format.mime, name) ?: return null
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                check(format.compress(bitmap, quality, out, exif))
            } ?: error("Output unavailable")
            uri
        } catch (e: Exception) {
            logFailure("StorageManager", e); context.contentResolver.delete(uri, null, null)
            null
        }
    }.getOrNull()

    suspend fun writeThumb(projectId: Long, index: Int, bitmap: Bitmap): String = writeBitmap(projectId, "thumbs", "t_$index.jpg", bitmap, ThumbSpec.JPEG_QUALITY)
    suspend fun writeAnalysis(projectId: Long, index: Int, bitmap: Bitmap): String =
        writeBitmap(projectId, ThumbSpec.ANALYSIS_FOLDER, ThumbSpec.analysisName(index), bitmap, ThumbSpec.ANALYSIS_QUALITY)
    suspend fun writeFrame(projectId: Long, index: Int, bitmap: Bitmap, format: String = "jpg", quality: Int = 92): String {
        val target = ImageFormat.working(format, GainmapOps.has(bitmap))
        return withContext(Dispatchers.IO) {
            writeBitmapFile(projectId, "frames", "f_$index.${target.ext}") { out -> check(target.compress(bitmap, quality, out)) }
        }
    }
    private suspend fun writeLosslessBitmap(projectId: Long, folder: String, name: String, bitmap: Bitmap): String = withContext(Dispatchers.IO) { writeBitmapFile(projectId, folder, name) { out -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) } }
    private suspend fun writeBitmap(projectId: Long, folder: String, name: String, bitmap: Bitmap, quality: Int): String = withContext(Dispatchers.IO) { writeBitmapFile(projectId, folder, name) { out -> check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) } }
    fun deleteQuietly(path: String?) {
        if (path == null) return
        runCatching { File(path).delete() }
    }

    suspend fun sweepOrphanFrameFiles(projectId: Long, knownIndexes: Set<Int>): Int = withContext(Dispatchers.IO) {
        var removed = 0
        for (folder in listOf("thumbs", "frames", ThumbSpec.ANALYSIS_FOLDER)) {
            val files = File(projectDir(projectId), folder).listFiles() ?: continue
            for (file in files) {
                if (!file.isFile) continue
                val orphan = file.name.endsWith(".tmp") || ORPHAN_NAME.matchEntire(file.name)?.let { it.groupValues[1].toIntOrNull() !in knownIndexes } == true
                if (orphan && runCatching { file.delete() }.getOrDefault(false)) removed++
            }
        }
        removed
    }

    private fun writeBitmapFile(projectId: Long, folder: String, name: String, writer: (FileOutputStream) -> Unit): String {
        val dir = File(projectDir(projectId), folder).also { it.mkdirs() }
        val file = File(dir, name)
        val temp = File(dir, "$name.tmp")
        try {
            FileOutputStream(temp).use(writer)
            if (file.exists()) file.delete()
            check(temp.renameTo(file))
            return file.absolutePath
        } catch (e: Exception) {
            temp.delete()
            if (availableBytes() < FRAME_WRITE_MIN_BYTES) {
                throw java.io.IOException("ENOSPC: no space left on device", e)
            }
            throw e
        }
    }
}

private val ORPHAN_NAME = Regex("""[tfa]_(\d+)\.[A-Za-z0-9]+""")
private const val UNKNOWN_SIZE_PIXELS = 3840L * 2160L
private const val THUMBNAIL_BYTES = 96L * 1024L
private const val SPACE_MARGIN_BYTES = 32L * 1024L * 1024L
private const val HEADROOM_BYTES = 150L * 1024L * 1024L
private const val FRAME_WRITE_MIN_BYTES = 8L * 1024L * 1024L
