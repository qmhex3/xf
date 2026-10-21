package com.frameout.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.ArrayDeque

class BitmapPool(private val maxEntries: Int = 8, private val maxBytes: () -> Long = { Long.MAX_VALUE }) {
    private val pool = ArrayDeque<Bitmap>(maxEntries)
    private var bytes = 0L

    @Synchronized
    fun obtain(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        val it = pool.iterator()
        while (it.hasNext()) {
            val b = it.next()
            if (b.width == width && b.height == height && b.config == config && !b.isRecycled) {
                it.remove()
                bytes -= b.allocationByteCount
                b.eraseColor(0)
                return b
            }
        }
        return Bitmap.createBitmap(width, height, config)
    }

    @Synchronized
    fun takeAtLeast(width: Int, height: Int): Bitmap? {
        val need = width.toLong() * height * BYTES_PER_PIXEL
        val it = pool.iterator()
        while (it.hasNext()) {
            val b = it.next()
            if (b.isRecycled) {
                it.remove()
                continue
            }
            if (b.isMutable && b.allocationByteCount >= need) {
                it.remove()
                bytes -= b.allocationByteCount
                return b
            }
        }
        return null
    }

    @Synchronized
    fun recycle(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return
        if (!bitmap.isMutable || bitmap.config != Bitmap.Config.ARGB_8888 || bitmap.allocationByteCount > maxBytes()) {
            bitmap.recycle()
            return
        }
        pool.addLast(bitmap)
        bytes += bitmap.allocationByteCount
        evict()
    }

    fun decodeFile(path: String, sample: Int, width: Int, height: Int): Bitmap? {
        val reuse = takeAtLeast(width, height)
        val options = decodeOptions(sample, reuse)
        val decoded = try {
            BitmapFactory.decodeFile(path, options)
        } catch (e: IllegalArgumentException) {
            reuse?.recycle()
            return BitmapFactory.decodeFile(path, decodeOptions(sample, null))
        }
        if (decoded == null) reuse?.recycle()
        return decoded
    }

    @Synchronized
    fun clear() {
        while (pool.isNotEmpty()) pool.removeFirst().recycle()
        bytes = 0L
    }

    @Synchronized
    fun size(): Int = pool.size

    private fun decodeOptions(sample: Int, reuse: Bitmap?) = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inMutable = true
        inBitmap = reuse
    }

    private fun evict() {
        val limit = maxBytes()
        while (pool.size > maxEntries || (bytes > limit && pool.isNotEmpty())) {
            val old = pool.removeFirst()
            bytes -= old.allocationByteCount
            old.recycle()
        }
    }

    private companion object {
        const val BYTES_PER_PIXEL = 4
    }
}
