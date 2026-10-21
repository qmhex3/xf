package com.frameout.engine.decode

import android.graphics.Bitmap
import android.net.Uri

interface AnimatedFrameSource {
    fun open(uri: Uri)
    fun close()
    fun frameCount(): Int
    fun videoWidth(): Int
    fun videoHeight(): Int
    fun durationUs(): Long
    fun ptsUs(index: Int): Long
    fun decodeFrame(index: Int): Pair<Long, Bitmap>?

    fun decodeNearest(targetUs: Long): Pair<Long, Bitmap>? {
        val total = frameCount()
        if (total <= 0) return null
        var low = 0
        var high = total - 1
        while (low < high) {
            val mid = (low + high) ushr 1
            if (ptsUs(mid) < targetUs) low = mid + 1 else high = mid
        }
        val before = (low - 1).coerceAtLeast(0)
        val chosen = if (kotlin.math.abs(ptsUs(before) - targetUs) <= kotlin.math.abs(ptsUs(low) - targetUs)) before else low
        return decodeFrame(chosen)
    }
}
