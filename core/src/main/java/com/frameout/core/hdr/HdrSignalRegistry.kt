package com.frameout.core.hdr

import android.graphics.Bitmap
import java.util.Collections
import java.util.WeakHashMap

object HdrSignalRegistry {
    private val signals = Collections.synchronizedMap(WeakHashMap<Bitmap, HdrSignal>())

    fun attach(bitmap: Bitmap, signal: HdrSignal) {
        signals[bitmap] = signal
    }

    fun peek(bitmap: Bitmap): HdrSignal? = if (bitmap.isRecycled) null else signals[bitmap]

    fun has(bitmap: Bitmap): Boolean = peek(bitmap) != null

    fun drop(bitmap: Bitmap) {
        signals.remove(bitmap)
    }
}
