package com.frameout.core.media

import android.net.Uri
import java.io.File

data class FolderEntry(
    val path: String,
    val name: String,
    val mime: String,
    val size: Long,
    val dateSec: Long,
    val bucketId: String,
    val bucketName: String,
    val kind: MediaKind,
    val readable: Boolean = true,
    val id: Long = 0L
) {
    val key: MediaKey get() = MediaKey(dateSec, id)

    fun toMediaItem(): MediaItem =
        MediaItem(id, Uri.fromFile(File(path)).toString(), name, mime, size, 0L, 0, 0, dateSec, bucketId, bucketName, kind)
}
