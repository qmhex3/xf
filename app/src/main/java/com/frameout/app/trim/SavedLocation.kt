package com.frameout.app.trim

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import android.provider.MediaStore

private const val STORAGE_AUTHORITY = "com.android.externalstorage.documents"
private const val ACTION_BROWSE = "android.provider.action.BROWSE"

internal fun SavedCapture.opensInViewer(): Boolean =
    mime.startsWith("image/") || mime.startsWith("video/") || mime == "application/pdf"

private fun SavedCapture.folderDocumentId(context: Context): String? {
    if (uri.authority == MediaStore.AUTHORITY) {
        val relative = runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()?.trim('/')
        return relative?.takeIf { it.isNotEmpty() }?.let { "primary:$it" }
    }
    if (uri.authority == STORAGE_AUTHORITY) {
        val id = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return null
        val slash = id.lastIndexOf('/')
        return if (slash > 0) id.substring(0, slash) else id.substringBefore(':') + ":"
    }
    return null
}

internal fun SavedCapture.showInFolder(context: Context) {
    val attempts = buildList {
        folderDocumentId(context)?.let { id ->
            val folder = DocumentsContract.buildDocumentUri(STORAGE_AUTHORITY, id)
            add(Intent(Intent.ACTION_VIEW).setDataAndType(folder, DocumentsContract.Document.MIME_TYPE_DIR))
            add(Intent(ACTION_BROWSE).setData(folder))
        }
        add(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        add(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
    attempts.any { tryLaunch(context, it) }
}

internal fun tryLaunch(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}
