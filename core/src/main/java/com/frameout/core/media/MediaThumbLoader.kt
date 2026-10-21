package com.frameout.core.media

import com.frameout.core.util.SmoothScale
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import com.frameout.core.util.ThumbCache

object MediaThumbLoader {
    private val misses = LruCache<String, Boolean>(2048)

    private fun memoryKey(item: MediaItem, maxSize: Int) = "media#${item.uri}#$maxSize"

    fun peek(item: MediaItem, maxSize: Int = 256): Bitmap? = ThumbCache.peekKey(memoryKey(item, maxSize))

    suspend fun load(context: Context, item: MediaItem, maxSize: Int = 256): Bitmap? {
        val key = memoryKey(item, maxSize)
        ThumbCache.peekKey(key)?.let { return it }
        if (isMissing(key)) return null
        val stored = ThumbCache.loadKeyed(key) { loadStored(context, item, maxSize) }
        if (stored != null) return stored
        val fallback = if (item.kind == MediaKind.Video) FfmpegThumbnailer.thumbnail(context, item, maxSize) else null
        if (fallback != null) {
            ThumbCache.putKey(key, fallback)
            ThumbDiskCache.write(context, ThumbDiskCache.keyOf(item, maxSize), fallback)
        } else {
            markMissing(key)
        }
        return fallback
    }

    private fun isMissing(key: String): Boolean = synchronized(misses) { misses.get(key) != null }

    private fun markMissing(key: String) {
        synchronized(misses) { misses.put(key, true) }
    }

    private fun loadStored(context: Context, item: MediaItem, maxSize: Int): Bitmap? {
        SystemThumbnails.load(context, item, maxSize)?.let { return it }
        val diskKey = ThumbDiskCache.keyOf(item, maxSize)
        ThumbDiskCache.read(context, diskKey)?.let { return it }
        return decode(context, item, maxSize)?.also { ThumbDiskCache.write(context, diskKey, it) }
    }

    private fun decode(context: Context, item: MediaItem, maxSize: Int): Bitmap? {
        return when (item.kind) {
            MediaKind.Video -> loadVideo(context, item.uri, maxSize)
            MediaKind.LivePhoto, MediaKind.MotionPhoto -> loadImage(context, item.uri, maxSize) ?: loadVideo(context, item.uri, maxSize)
            MediaKind.Gif, MediaKind.Animated, MediaKind.Image -> loadImage(context, item.uri, maxSize)
        }
    }

    private fun loadVideo(context: Context, value: String, maxSize: Int): Bitmap? = runCatching {
        val uri = Uri.parse(value)
        VideoThumbLookup.provider?.firstFrame(uri)?.let { return@runCatching scale(it, maxSize) }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { scale(it, maxSize) }
        } finally { retriever.release() }
    }.getOrNull()

    private fun loadImage(context: Context, value: String, maxSize: Int): Bitmap? = runCatching {
        val uri = Uri.parse(value)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxSize)
        val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    private fun scale(source: Bitmap, maxSize: Int): Bitmap {
        if (source.width <= maxSize && source.height <= maxSize) return source
        val factor = maxSize.toFloat() / maxOf(source.width, source.height)
        val result = SmoothScale.scaled(source, (source.width * factor).toInt().coerceAtLeast(1), (source.height * factor).toInt().coerceAtLeast(1), true)
        if (result !== source) source.recycle()
        return result
    }

    private fun sampleSize(width: Int, height: Int, maxSize: Int): Int {
        var value = 1
        while (width / value > maxSize * 2 || height / value > maxSize * 2) value *= 2
        return value
    }
}
