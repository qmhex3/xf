package com.frameout.core.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.frameout.core.util.logFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GalleryBackup(private val context: Context) {
    class Snapshot(val file: File, val displayName: String, val mime: String, val relativePath: String, val dateTaken: Long)

    private val dir: File get() = File(context.cacheDir, DIR).also { it.mkdirs() }

    suspend fun capture(uri: String, displayName: String, createdAt: Long): Snapshot? = withContext(Dispatchers.IO) {
        sweep()
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return@withContext null
        val resolver = context.contentResolver
        val mime = runCatching { resolver.getType(parsed) }.getOrNull() ?: "image/jpeg"
        val file = File(dir, "undo_${System.nanoTime()}.bin")
        val copied = runCatching {
            resolver.openInputStream(parsed)?.use { input -> file.outputStream().use { input.copyTo(it) } } != null
        }.onFailure { logFailure("GalleryBackup", it) }.getOrDefault(false)
        if (!copied || file.length() == 0L) {
            file.delete()
            return@withContext null
        }
        Snapshot(file, displayName, mime, relativePathOf(parsed), createdAt)
    }

    suspend fun restore(snapshot: Snapshot): Uri? = withContext(Dispatchers.IO) {
        if (!snapshot.file.exists()) return@withContext null
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, snapshot.displayName)
            put(MediaStore.Images.Media.MIME_TYPE, snapshot.mime)
            put(MediaStore.Images.Media.DATE_TAKEN, snapshot.dateTaken)
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, snapshot.relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: return@withContext null
        try {
            resolver.openOutputStream(uri)?.use { out -> snapshot.file.inputStream().use { it.copyTo(out) } } ?: error("Output unavailable")
            if (Build.VERSION.SDK_INT >= 29) resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            uri
        } catch (e: Exception) {
            logFailure("GalleryBackup", e)
            resolver.delete(uri, null, null)
            null
        }
    }

    fun discard(snapshot: Snapshot?) {
        snapshot?.file?.delete()
    }

    private fun relativePathOf(uri: Uri): String {
        if (Build.VERSION.SDK_INT < 29 || uri.authority != MediaStore.AUTHORITY) return DEFAULT_PATH
        val found = runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull()
        return found?.takeIf { it.isNotBlank() } ?: DEFAULT_PATH
    }

    private fun sweep() {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }

    private companion object {
        const val DIR = "frameout_undo"
        const val DEFAULT_PATH = "Pictures/FrameOut/"
        const val MAX_AGE_MS = 60L * 60L * 1000L
    }
}
