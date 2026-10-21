package com.frameout.core.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns

object SourceInfo {
    fun name(context: Context, uriString: String?): String {
        if (uriString.isNullOrBlank()) return FALLBACK
        val raw = runCatching {
            context.contentResolver.query(Uri.parse(uriString), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
            }
        }.getOrNull() ?: Uri.parse(uriString).lastPathSegment
        return raw.orEmpty().substringAfterLast('/').substringBeforeLast('.').ifBlank { FALLBACK }
    }

    private const val FALLBACK = "video"
}

object SourceDate {
    fun read(context: Context, uriString: String?): Long? = runCatching {
        if (uriString.isNullOrBlank()) return null
        val uri = Uri.parse(uriString)
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATE_MODIFIED, "date_taken", DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { c ->
            if (!c.moveToFirst()) return null
            val takenIndex = c.getColumnIndex("date_taken")
            val modifiedIndex = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val documentIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val taken = if (takenIndex >= 0) c.getLong(takenIndex) else 0L
            val modified = if (modifiedIndex >= 0) c.getLong(modifiedIndex) * 1000L else 0L
            val document = if (documentIndex >= 0) c.getLong(documentIndex) else 0L
            taken.takeIf { it > 0L } ?: document.takeIf { it > 0L } ?: modified.takeIf { it > 0L }
        }
    }.getOrNull()
}
