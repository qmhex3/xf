package com.frameout.app.batch

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.frameout.core.storage.SaveLocation

object BatchNaming {
    fun displayName(context: Context, uri: String): String {
        val parsed = Uri.parse(uri)
        val queried = runCatching {
            context.contentResolver.query(parsed, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
        val raw = queried ?: parsed.lastPathSegment?.substringAfterLast('/') ?: ""
        return raw.substringBeforeLast('.', raw).ifBlank { "video" }
    }

    fun uniqueFolder(name: String, taken: MutableSet<String>): String {
        val base = SaveLocation.folderName(name)
        var candidate = base
        var counter = 2
        while (!taken.add(candidate.lowercase())) candidate = "$base $counter".also { counter++ }
        return candidate
    }
}
