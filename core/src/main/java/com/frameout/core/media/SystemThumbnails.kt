package com.frameout.core.media

import com.frameout.core.util.SmoothScale
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.RequiresApi

internal object SystemThumbnails {
    fun load(context: Context, item: MediaItem, maxSize: Int): Bitmap? = runCatching {
        if (item.id <= 0L) return null
        val uri = Uri.parse(item.uri)
        if (uri.scheme != "content") return null
        val bitmap = if (Build.VERSION.SDK_INT >= 29) viaProvider(context, uri, maxSize) else viaLegacy(context, item)
        bitmap?.let { limit(it, maxSize) }
    }.getOrNull()

    @RequiresApi(29)
    private fun viaProvider(context: Context, uri: Uri, maxSize: Int): Bitmap? =
        context.contentResolver.loadThumbnail(uri, Size(maxSize, maxSize), null)

    @Suppress("DEPRECATION")
    private fun viaLegacy(context: Context, item: MediaItem): Bitmap? {
        val resolver = context.contentResolver
        return if (item.kind == MediaKind.Video) {
            MediaStore.Video.Thumbnails.getThumbnail(resolver, item.id, MediaStore.Video.Thumbnails.MINI_KIND, null)
        } else {
            MediaStore.Images.Thumbnails.getThumbnail(resolver, item.id, MediaStore.Images.Thumbnails.MINI_KIND, null)
        }
    }

    private fun limit(source: Bitmap, maxSize: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxSize + maxSize / 2) return source
        val factor = maxSize.toFloat() / longest
        val width = (source.width * factor).toInt().coerceAtLeast(1)
        val height = (source.height * factor).toInt().coerceAtLeast(1)
        val scaled = SmoothScale.scaled(source, width, height, true)
        if (scaled !== source) source.recycle()
        return scaled
    }
}
