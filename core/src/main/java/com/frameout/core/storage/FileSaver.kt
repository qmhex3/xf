package com.frameout.core.storage

import com.frameout.core.util.logged
import com.frameout.core.util.logFailure
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

class FileSaver(private val context: Context) {
    suspend fun save(name: String, mime: String, relativePath: String, treeUri: String?, subfolder: String = "", writer: (OutputStream) -> Unit): Uri? = withContext(Dispatchers.IO) {
        val path = SaveLocation.join(relativePath, subfolder)
        runCatching {
            val tree = SaveFolderGuard.resolve(context, treeUri)
            val viaTree = tree?.let { runCatching { saveToTree(name, mime, Uri.parse(it), subfolder, writer) }.logged("FileSaver").getOrNull() }
            if (tree != null && viaTree == null) SaveFolderGuard.fellBack()
            if (viaTree != null) viaTree
            else if (Build.VERSION.SDK_INT >= 29) saveToMediaStore(name, mime, path, writer)
            else saveLegacy(name, mime, path, writer)
        }.logged("FileSaver").getOrNull()
    }

    @RequiresApi(29)
    private fun collection(mime: String): Uri = when {
        mime.startsWith("image/") -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        mime.startsWith("video/") -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }

    @RequiresApi(29)
    private fun saveToMediaStore(name: String, mime: String, relativePath: String, writer: (OutputStream) -> Unit): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath.trim('/').replace("..", "") + "/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection(mime), values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use(writer) ?: error("Output unavailable")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            uri
        } catch (e: Exception) {
            logFailure("FileSaver", e)
            resolver.delete(uri, null, null)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(name: String, mime: String, relativePath: String, writer: (OutputStream) -> Unit): Uri? {
        val root = Environment.getExternalStorageDirectory()
        val dir = File(root, relativePath.trim('/').replace("..", ""))
        dir.mkdirs()
        val file = uniqueFile(dir, name)
        try {
            file.outputStream().use(writer)
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime), null)
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun uniqueFile(dir: File, name: String): File {
        var file = File(dir, name)
        if (!file.exists()) return file
        val stem = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var counter = 1
        while (file.exists()) {
            file = File(dir, if (ext.isEmpty()) "${stem}_$counter" else "${stem}_$counter.$ext")
            counter++
        }
        return file
    }

    private fun saveToTree(name: String, mime: String, tree: Uri, subfolder: String, writer: (OutputStream) -> Unit): Uri? {
        val parent = SaveLocation.treeParent(context.contentResolver, tree, subfolder)
        val uri = DocumentsContract.createDocument(context.contentResolver, parent, mime, name) ?: return null
        return try {
            context.contentResolver.openOutputStream(uri)?.use(writer) ?: error("Output unavailable")
            uri
        } catch (e: Exception) {
            logFailure("FileSaver", e)
            context.contentResolver.delete(uri, null, null)
            null
        }
    }
}
