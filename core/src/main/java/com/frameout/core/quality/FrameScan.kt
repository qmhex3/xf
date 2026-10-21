package com.frameout.core.quality

import android.graphics.Bitmap
import com.frameout.core.db.FrameDao
import com.frameout.core.db.FrameEntity
import com.frameout.core.util.ThumbCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object FrameScan {
    private const val PAGE = 400

    suspend fun <T> mapThumbnails(dao: FrameDao, projectId: Long, accept: (FrameEntity) -> Boolean = { true }, block: suspend (FrameEntity, Bitmap) -> T): List<T> =
        withContext(Dispatchers.IO) {
            val out = ArrayList<T>()
            var offset = 0
            while (true) {
                val page = dao.pageActive(projectId, PAGE, offset)
                if (page.isEmpty()) break
                page.forEach { frame ->
                    ensureActive()
                    if (!accept(frame)) return@forEach
                    val bitmap = ThumbCache.decodePooled(frame.thumbnailPath ?: frame.fullPath) ?: return@forEach
                    try {
                        out += block(frame, bitmap)
                    } finally {
                        ThumbCache.release(bitmap)
                    }
                }
                offset += page.size
            }
            out
        }
}
