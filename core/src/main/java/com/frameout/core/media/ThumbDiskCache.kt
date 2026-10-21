package com.frameout.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

internal object ThumbDiskCache {
    private const val DIR = "frameout_thumbs"
    private const val MAX_BYTES = 48L * 1024L * 1024L
    private const val TRIM_EVERY = 40
    private val writes = AtomicInteger()

    fun keyOf(item: MediaItem, maxSize: Int): String {
        val raw = "${item.uri}|${item.sizeBytes}|${item.dateAdded}|$maxSize"
        return MessageDigest.getInstance("SHA-1").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun read(context: Context, key: String): Bitmap? {
        val file = File(dir(context), "$key.jpg")
        if (!file.isFile) return null
        val bitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        if (bitmap == null) file.delete() else file.setLastModified(System.currentTimeMillis())
        return bitmap
    }

    fun write(context: Context, key: String, bitmap: Bitmap) {
        runCatching {
            val folder = dir(context)
            val temp = File(folder, "$key.tmp")
            temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            if (!temp.renameTo(File(folder, "$key.jpg"))) temp.delete()
            if (writes.incrementAndGet() % TRIM_EVERY == 0) trim(folder)
        }
    }

    private fun dir(context: Context): File = File(context.cacheDir, DIR).also { it.mkdirs() }

    private fun trim(folder: File) {
        val files = folder.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= MAX_BYTES) break
            total -= file.length()
            file.delete()
        }
    }
}
