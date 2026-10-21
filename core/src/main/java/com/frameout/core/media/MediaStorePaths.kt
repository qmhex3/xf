package com.frameout.core.media

import android.content.Context
import android.provider.MediaStore

internal object MediaStorePaths {
    private const val CHUNK = 300

    @Suppress("DEPRECATION")
    private const val DATA = MediaStore.MediaColumns.DATA

    fun indexedAmong(context: Context, paths: List<String>): Set<String>? = runCatching {
        val found = HashSet<String>()
        paths.chunked(CHUNK).forEach { chunk -> queryChunk(context, chunk, found) }
        found
    }.getOrNull()

    private fun queryChunk(context: Context, chunk: List<String>, into: MutableSet<String>) {
        val marks = chunk.joinToString(",") { "?" }
        val type = MediaStore.Files.FileColumns.MEDIA_TYPE
        val selection = "$DATA IN ($marks) AND ($type=? OR $type=?)"
        val args = chunk + listOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()
        )
        context.contentResolver.query(MediaStore.Files.getContentUri("external"), arrayOf(DATA), selection, args.toTypedArray(), null)?.use { cursor ->
            while (cursor.moveToNext()) cursor.getString(0)?.let(into::add)
        }
    }
}
