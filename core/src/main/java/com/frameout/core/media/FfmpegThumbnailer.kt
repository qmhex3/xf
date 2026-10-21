package com.frameout.core.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.frameout.codecpack.CodecPack
import com.frameout.codecpack.FfmpegBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

internal object FfmpegThumbnailer {
    private const val MAX_FAILED = 512
    private val gate = Semaphore(2)
    private val failed = LinkedHashSet<String>()

    suspend fun thumbnail(context: Context, item: MediaItem, maxSize: Int): Bitmap? {
        val key = ThumbDiskCache.keyOf(item, maxSize)
        val stored = withContext(Dispatchers.IO) { ThumbDiskCache.read(context, key) }
        if (stored != null) return stored
        if (isFailed(key)) return null
        return gate.withPermit {
            currentCoroutineContext().ensureActive()
            withContext(Dispatchers.IO) { create(context, item, key, maxSize) }
        }
    }

    private fun create(context: Context, item: MediaItem, key: String, maxSize: Int): Bitmap? {
        if (!CodecPack.load(context)) return null
        val bitmap = render(context, Uri.parse(item.uri), maxSize)
        if (bitmap == null) markFailed(key) else ThumbDiskCache.write(context, key, bitmap)
        return bitmap
    }

    private fun render(context: Context, uri: Uri, maxSize: Int): Bitmap? {
        val descriptor = openDescriptor(context, uri) ?: return null
        val path = "/proc/self/fd/${descriptor.fd}"
        return try {
            decode(path, maxSize)
        } finally {
            runCatching { FfmpegBridge.nativeCloseSession(path) }
            runCatching { descriptor.close() }
        }
    }

    private fun openDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? = runCatching {
        if (uri.scheme == "file") ParcelFileDescriptor.open(File(uri.path ?: return null), ParcelFileDescriptor.MODE_READ_ONLY)
        else context.contentResolver.openFileDescriptor(uri, "r")
    }.getOrNull()

    private fun decode(path: String, maxSize: Int): Bitmap? = runCatching {
        val width = FfmpegBridge.nativeWidth(path)
        val height = FfmpegBridge.nativeHeight(path)
        if (width <= 0 || height <= 0) return null
        val (w, h) = fit(width, height, maxSize)
        val rgba = ByteBuffer.allocateDirect(w * h * 4)
        val rc = FfmpegBridge.nativeDecodeFrame(path, 0, rgba, w, h, LongArray(1), BooleanArray(1), IntArray(1), LongArray(1))
        if (rc != 0) return null
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            rgba.rewind()
            it.copyPixelsFromBuffer(rgba)
        }
    }.getOrNull()

    internal fun fit(width: Int, height: Int, maxSize: Int): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (longest <= maxSize) return width to height
        val w = (width.toLong() * maxSize / longest).toInt().coerceAtLeast(1)
        val h = (height.toLong() * maxSize / longest).toInt().coerceAtLeast(1)
        return w to h
    }

    private fun isFailed(key: String): Boolean = synchronized(failed) { key in failed }

    private fun markFailed(key: String) {
        synchronized(failed) {
            failed.add(key)
            if (failed.size > MAX_FAILED) failed.remove(failed.first())
        }
    }
}
