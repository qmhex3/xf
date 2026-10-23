package com.frameout.app

import com.frameout.core.util.SmoothScale
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.frameout.core.db.ProjectDao
import com.frameout.core.storage.StorageManager
import com.frameout.core.storage.ThumbSpec
import com.frameout.engine.decode.FfmpegFrameSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ProjectCover {
    suspend fun create(
        context: Context,
        storage: StorageManager,
        dao: ProjectDao,
        projectId: Long,
        uri: Uri,
        startMs: Long = 0L
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val shared = if (startMs <= 0L) com.frameout.core.media.MediaThumbLoader.loadForUri(context, uri.toString())?.copy(Bitmap.Config.ARGB_8888, false) else null
                val file = (shared ?: runCatching { grab(context, uri, startMs) }.getOrNull() ?: grabFfmpeg(context, uri, startMs))?.let { save(storage, projectId, it) } ?: return@runCatching
                dao.setThumbnail(projectId, file.absolutePath, System.currentTimeMillis())
            }
        }
    }

    private fun grab(context: Context, uri: Uri, startMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val timeUs = when {
                startMs > 0L -> startMs * 1000L
                durationMs > 0L -> durationMs * 1000L / 10L
                else -> 0L
            }
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            scale(frame)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun grabFfmpeg(context: Context, uri: Uri, startMs: Long): Bitmap? = runCatching {
        val source = FfmpegFrameSource(context.applicationContext)
        try {
            source.open(uri)
            source.decodeNearest(startMs.coerceAtLeast(0L) * 1000L)?.second?.let(::scale)
        } finally {
            runCatching { source.close() }
        }
    }.getOrNull()

    private fun scale(src: Bitmap): Bitmap {
        val max = ThumbSpec.MAX_SIDE
        if (src.width <= max && src.height <= max) return src
        val s = max.toFloat() / maxOf(src.width, src.height)
        val out = SmoothScale.scaled(src, (src.width * s).toInt().coerceAtLeast(1), (src.height * s).toInt().coerceAtLeast(1), true)
        if (out !== src) src.recycle()
        return out
    }

    private fun save(storage: StorageManager, projectId: Long, bitmap: Bitmap): File? {
        val file = File(storage.thumbsDir(projectId), "cover.jpg")
        return try {
            FileOutputStream(file).use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, ThumbSpec.JPEG_QUALITY, it)) }
            file
        } catch (e: Exception) {
            file.delete()
            null
        } finally {
            bitmap.recycle()
        }
    }
}
