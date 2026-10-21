package com.frameout.engine.extract
import com.frameout.core.util.SmoothScale
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
fun ffmpegDurationMs(context: Context, uri: Uri): Long {
    val r = MediaMetadataRetriever()
    return try { r.setDataSource(context, uri); r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.coerceAtLeast(0) ?: 0 } finally { r.release() }
}
fun ffmpegThumb(src: Bitmap): Bitmap {
    val max = com.frameout.core.storage.ThumbSpec.MAX_SIDE
    if (src.width <= max && src.height <= max) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, false)
    val factor = max.toFloat() / maxOf(src.width, src.height)
    return SmoothScale.scaled(src, (src.width * factor).toInt().coerceAtLeast(1), (src.height * factor).toInt().coerceAtLeast(1), true)
}
