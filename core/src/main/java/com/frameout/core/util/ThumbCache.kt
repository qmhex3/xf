package com.frameout.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ThumbCache {
    const val DEFAULT_SIZE = 256

    private const val MAX_DECODES = 4
    private const val TRIM_BACKGROUND = 40
    private const val TRIM_MODERATE = 60
    private const val TRIM_COMPLETE = 80

    private val decoder: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(MAX_DECODES)
    private val gate = AdaptiveGate(MAX_DECODES) { DeviceLimits.thumbDecodes() }

    val pool = BitmapPool(maxBytes = DeviceLimits::poolBytes)

    private val shared = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 5L).coerceIn(32L * 1024L * 1024L, 160L * 1024L * 1024L).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun peek(path: String?, maxSize: Int = 256): Bitmap? {
        if (path.isNullOrBlank()) return null
        return shared.get("$path#$maxSize")
    }

    fun peekKey(key: String): Bitmap? = shared.get(key)

    fun putKey(key: String, bitmap: Bitmap) {
        shared.put(key, bitmap)
    }

    suspend fun load(path: String?, maxSize: Int = 256): Bitmap? {
        peek(path, maxSize)?.let { return it }
        if (path.isNullOrBlank()) return null
        return withContext(decoder) { gate.withPermit { getShared(path, maxSize) } }
    }

    suspend fun loadKeyed(key: String, block: () -> Bitmap?): Bitmap? {
        shared.get(key)?.let { return it }
        return withContext(decoder) {
            gate.withPermit { shared.get(key) ?: block()?.also { shared.put(key, it) } }
        }
    }

    fun getShared(path: String?, maxSize: Int = 256): Bitmap? {
        if (path.isNullOrBlank()) return null
        val key = "$path#$maxSize"
        shared.get(key)?.let { return it }
        val loaded = get(path, maxSize) ?: return null
        shared.put(key, loaded)
        return loaded
    }

    fun get(path: String?, maxSize: Int = 256): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxSize)
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 })
    }

    fun decodePooled(path: String?, maxSize: Int = 256): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxSize)
        val width = (bounds.outWidth + sample - 1) / sample
        val height = (bounds.outHeight + sample - 1) / sample
        return pool.decodeFile(file.absolutePath, sample, width, height)
    }

    fun release(bitmap: Bitmap?) = pool.recycle(bitmap)

    fun trim(level: Int) {
        if (level >= TRIM_COMPLETE) {
            shared.evictAll()
        } else if (level >= TRIM_MODERATE) {
            shared.trimToSize(shared.size() / 2)
        }
        if (level >= TRIM_BACKGROUND) pool.clear()
    }

    fun peekSharp(path: String?, px: Int): Bitmap? {
        if (path.isNullOrBlank()) return null
        return shared.get(sharpKey(path, px))
    }

    suspend fun loadSharp(path: String?, px: Int): Bitmap? {
        if (path.isNullOrBlank()) return null
        val key = sharpKey(path, px)
        shared.get(key)?.let { return it }
        return withContext(decoder) {
            gate.withPermit { shared.get(key) ?: decodeSharp(path, px)?.also { shared.put(key, it) } }
        }
    }

    fun decodeSharp(path: String, px: Int): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (minOf(bounds.outWidth, bounds.outHeight) <= DEFAULT_SIZE) return null
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, px)
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 })
    }

    private fun sharpKey(path: String, px: Int): String = "$path#hq$px"

    private fun sampleSize(width: Int, height: Int, maxSize: Int): Int {
        val shortSide = minOf(width, height)
        var value = 1
        while (shortSide / (value * 2) >= maxSize) value *= 2
        return value
    }
}
