package com.frameout.core.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

data class LatestVideo(val id: Long, val uri: Uri, val addedMs: Long, val durationMs: Long)

object LatestVideoFinder {
    private const val SCAN_LIMIT = 8
    private const val MIN_DURATION_MS = 1_000L
    private const val OWN_FOLDER = "frameout"

    @Suppress("DEPRECATION")
    fun find(context: Context, skip: Set<String> = emptySet()): LatestVideo? {
        if (!PermissionHelper.canReadVideos(context)) return null
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val pathColumn = if (Build.VERSION.SDK_INT >= 29) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            pathColumn
        )
        return runCatching {
            context.contentResolver.query(collection, projection, null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC")?.use { cursor ->
                var scanned = 0
                while (cursor.moveToNext() && scanned < SCAN_LIMIT) {
                    scanned++
                    val id = cursor.getLong(0)
                    val added = cursor.getLong(1) * 1000L
                    val duration = cursor.getLong(2)
                    val path = cursor.getString(3).orEmpty().lowercase()
                    val uri = ContentUris.withAppendedId(collection, id)
                    if (duration in 0 until MIN_DURATION_MS) continue
                    if (path.contains(OWN_FOLDER)) continue
                    if (uri.toString() in skip) continue
                    return@use LatestVideo(id, uri, added, duration)
                }
                null
            }
        }.getOrNull()
    }
}
