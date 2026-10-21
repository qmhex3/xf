package com.frameout.core.media

import android.content.Context
import android.net.Uri
import android.util.LruCache
import java.io.FileInputStream

object ContentIdentifierReader {
    private const val MAX_BYTES = 1024 * 1024
    private const val CACHE_SIZE = 1024
    private const val MISSING = ""
    private val cache = LruCache<String, String>(CACHE_SIZE)
    private val patterns = listOf(
        Regex("(?:com\\.apple\\.quicktime\\.content\\.identifier|assetIdentifier)[^0-9A-Za-z-]{0,64}([0-9A-Fa-f-]{8,64})"),
        Regex("(?:content\\.identifier|contentIdentifier)[^0-9A-Za-z-]{0,64}([0-9A-Fa-f-]{8,64})")
    )

    fun read(context: Context, uri: Uri): String? {
        val key = uri.toString()
        synchronized(cache) { cache.get(key) }?.let { return it.takeIf { value -> value != MISSING } }
        val value = scan(context, uri)
        synchronized(cache) { cache.put(key, value ?: MISSING) }
        return value
    }

    private fun scan(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { stream ->
                val channel = stream.channel
                val size = channel.size()
                find(channel, 0L, minOf(size, MAX_BYTES.toLong()).toInt())
                    ?: if (size > MAX_BYTES) find(channel, size - MAX_BYTES, MAX_BYTES) else null
            }
        }
    }.getOrNull()

    private fun find(channel: java.nio.channels.FileChannel, position: Long, length: Int): String? {
        if (length <= 0) return null
        val buffer = java.nio.ByteBuffer.allocate(length)
        var offset = position
        while (buffer.hasRemaining()) {
            val count = channel.read(buffer, offset)
            if (count <= 0) break
            offset += count
        }
        LiveContentId.parse(buffer.array(), buffer.position())?.let { return it }
        val text = String(buffer.array(), 0, buffer.position(), Charsets.ISO_8859_1)
        return patterns.asSequence().mapNotNull { it.find(text)?.groupValues?.getOrNull(1) }.firstOrNull()?.lowercase()
    }
}
