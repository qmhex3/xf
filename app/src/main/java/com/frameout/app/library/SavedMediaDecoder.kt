package com.frameout.app.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.frameout.core.db.SavedKind
import com.frameout.core.db.savedKindOf
import com.frameout.core.media.AnimatedImageProbe
import com.frameout.core.util.logFailure
import java.io.File
import java.io.FileOutputStream

internal class SavedMediaDecoder(private val context: Context) {
    private val resolver get() = context.contentResolver

    fun isMoving(uri: Uri, name: String): Boolean =
        savedKindOf(name) != SavedKind.Photo || isVideo(uri) || isAnimatedImage(uri, name)

    private fun isAnimatedImage(uri: Uri, name: String): Boolean {
        val mime = runCatching { resolver.getType(uri) }.getOrNull().orEmpty()
        if (!AnimatedImageProbe.isCandidate(mime, name) && !AnimatedImageProbe.isPngCandidate(mime, name)) return false
        val size = runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: -1L
        return AnimatedImageProbe.isAnimated(context, uri, mime, name, size, 0L)
    }

    fun isVideo(uri: Uri): Boolean =
        runCatching { resolver.getType(uri) }.getOrNull()?.startsWith("video/") == true

    fun orientation(uri: Uri): Int = runCatching {
        resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    fun probe(uri: Uri): Pair<Int, Int>? {
        val bounds = imageBounds(uri)
        if (bounds != null) {
            val swap = orientation(uri) in SWAPPING
            return if (swap) bounds.second to bounds.first else bounds
        }
        return videoBounds(uri)
    }

    fun decode(uri: Uri, name: String, side: Int): Bitmap? {
        val video = isVideo(uri)
        val bitmap = (if (video) null else decodeStill(uri, side)) ?: decodeVideo(uri, side) ?: return null
        return if (video || isMoving(uri, name)) withPlayBadge(bitmap) else bitmap
    }

    fun stageTo(uri: Uri, name: String, side: Int, target: File): Boolean {
        val decoded = decode(uri, name, side) ?: return false
        val bitmap = flatten(decoded)
        return try {
            FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.JPEG, STAGE_QUALITY, it) }
        } catch (e: Exception) {
            logFailure("SavedMediaDecoder", e)
            false
        } finally {
            bitmap.recycle()
        }
    }

    private fun flatten(source: Bitmap): Bitmap {
        if (!source.hasAlpha()) return source
        val flat = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flat)
        canvas.drawColor(0xFFFFFFFF.toInt())
        canvas.drawBitmap(source, 0f, 0f, null)
        source.recycle()
        return flat
    }

    private fun decodeStill(uri: Uri, side: Int): Bitmap? {
        val bounds = imageBounds(uri)
        val raw = if (bounds != null) decodeBitmapFactory(uri, bounds, side) else null
        val bitmap = raw ?: decodeImageDecoder(uri, side) ?: return null
        return applyOrientation(bitmap, orientation(uri))
    }

    private fun decodeBitmapFactory(uri: Uri, bounds: Pair<Int, Int>, side: Int): Bitmap? {
        var sample = 1
        while (maxOf(bounds.first, bounds.second) / sample > side * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return runCatching { resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } }
            .onFailure { logFailure("SavedMediaDecoder", it) }
            .getOrNull()
    }

    private fun decodeImageDecoder(uri: Uri, side: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > side * 2) {
                    val scale = (side * 2f) / longest
                    decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                }
            }
        }.onFailure { logFailure("SavedMediaDecoder", it) }.getOrNull()
    }

    private fun decodeVideo(uri: Uri, side: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationUs = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) * 1000L
            val at = if (durationUs > 0) durationUs / POSTER_DIVISOR else 0L
            val frame = retriever.getFrameAtTime(at, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            downscale(frame, side * 2)
        } catch (e: Exception) {
            logFailure("SavedMediaDecoder", e)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun videoBounds(uri: Uri): Pair<Int, Int>? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: return null
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: return null
            val turned = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (w <= 0 || h <= 0) null else if (turned == 90 || turned == 270) h to w else w to h
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun imageBounds(uri: Uri): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } }
        return if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
    }

    private fun downscale(source: Bitmap, limit: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= limit) return source
        val scale = limit.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
        if (scaled !== source) source.recycle()
        return scaled
    }

    private fun applyOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return source
        }
        val turned = runCatching { Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true) }.getOrNull() ?: return source
        if (turned !== source) source.recycle()
        return turned
    }

    private fun withPlayBadge(source: Bitmap): Bitmap {
        val target = if (source.isMutable && source.config == Bitmap.Config.ARGB_8888) source else {
            val copy = source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
            if (copy !== source) source.recycle()
            copy
        }
        val canvas = Canvas(target)
        val radius = (minOf(target.width, target.height) * BADGE_RATIO).coerceAtLeast(BADGE_MIN)
        val cx = target.width / 2f
        val cy = target.height / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = BADGE_DISC
        canvas.drawCircle(cx, cy, radius, paint)
        paint.color = BADGE_GLYPH
        val half = radius * GLYPH_RATIO
        val path = Path().apply {
            moveTo(cx - half * 0.6f, cy - half)
            lineTo(cx - half * 0.6f, cy + half)
            lineTo(cx + half, cy)
            close()
        }
        canvas.drawPath(path, paint)
        return target
    }

    private companion object {
        val SWAPPING = setOf(
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270
        )
        const val STAGE_QUALITY = 90
        const val POSTER_DIVISOR = 10L
        const val BADGE_RATIO = 0.14f
        const val BADGE_MIN = 14f
        const val GLYPH_RATIO = 0.5f
        const val BADGE_DISC = 0xAA000000.toInt()
        const val BADGE_GLYPH = 0xFFFFFFFF.toInt()
    }
}
