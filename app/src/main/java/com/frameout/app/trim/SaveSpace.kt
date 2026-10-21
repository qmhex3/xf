package com.frameout.app.trim

import android.content.Context
import android.net.Uri
import com.frameout.app.FrameOutApp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

internal object SaveSpace {
    const val WEBP_FRAME_BYTES = 60_000L
    const val GIF_FRAME_BYTES = 150_000L
    const val STILL_BYTES = 4_000_000L
    private const val CACHE_HEADROOM = 8_000_000L
    private const val REENCODE_FACTOR_PCT = 120L
    private const val WATCH_MS = 400L
    private const val MAX_PERCENT = 95

    fun clipBytes(context: Context, uri: Uri, spanMs: Long, durationMs: Long): Long {
        if (spanMs <= 0L || durationMs <= 0L) return 0L
        val size = runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: return 0L
        if (size <= 0L) return 0L
        return (size.toDouble() * spanMs.coerceAtMost(durationMs) / durationMs).toLong() * REENCODE_FACTOR_PCT / 100L
    }

    fun fits(context: Context, treeUri: String?, bytes: Long): Boolean {
        if (bytes <= 0L) return true
        val storage = (context.applicationContext as FrameOutApp).container.storage
        return context.cacheDir.usableSpace > bytes + CACHE_HEADROOM && storage.hasDestinationRoom(treeUri, bytes)
    }

    fun cacheLow(context: Context): Boolean = context.cacheDir.usableSpace < CACHE_HEADROOM

    fun isNoSpace(error: Throwable): Boolean = generateSequence(error) { it.cause }.any {
        it is java.io.IOException && (it.message?.contains("ENOSPC") == true || it.message?.contains("No space left", ignoreCase = true) == true)
    }

    suspend fun <T> watchFile(file: File, expectedBytes: Long, onProgress: (Int) -> Unit, work: suspend () -> T): T = coroutineScope {
        val watcher = if (expectedBytes > 0L) launch {
            while (isActive) {
                delay(WATCH_MS)
                onProgress((file.length() * MAX_PERCENT / expectedBytes).toInt().coerceIn(0, MAX_PERCENT))
            }
        } else null
        try {
            work()
        } finally {
            watcher?.cancel()
        }
    }

    fun percentOf(done: Int, total: Int): Int = if (total <= 0) 0 else (done * MAX_PERCENT / total).coerceIn(0, MAX_PERCENT)
}
