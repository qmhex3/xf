package com.frameout.core.storage

import android.net.Uri
import android.provider.MediaStore
import com.frameout.core.db.FrameDao
import com.frameout.core.db.SavedImageDao
import com.frameout.core.db.deleteManyChunked
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class SavedReconciler(
    private val storage: StorageManager,
    private val saved: SavedImageDao,
    private val frames: FrameDao
) {
    suspend fun prune(): Int = withContext(Dispatchers.IO) {
        val canSeeMedia = storage.canReadMedia()
        val dead = ArrayList<Long>()
        var cursor = 0L
        while (true) {
            val page = saved.after(cursor, PAGE)
            if (page.isEmpty()) break
            cursor = page.last().id
            for (row in page) {
                ensureActive()
                if (isGone(row.uri, canSeeMedia) && !recoverable(row.sourceFrameId)) dead += row.id
            }
        }
        if (dead.isNotEmpty()) saved.deleteManyChunked(dead)
        dead.size
    }

    private fun isGone(uri: String, canSeeMedia: Boolean): Boolean {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull()
        if (parsed == null || parsed.scheme.isNullOrBlank()) return true
        if (parsed.authority == MediaStore.AUTHORITY && !canSeeMedia) return false
        return storage.uriExists(uri) == false
    }

    private suspend fun recoverable(sourceFrameId: Long?): Boolean =
        sourceFrameId != null && frames.getById(sourceFrameId) != null

    private companion object {
        const val PAGE = 200
    }
}
