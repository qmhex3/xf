package com.frameout.core.util

import android.graphics.Bitmap

object SmoothScale {
    fun scaled(src: Bitmap, width: Int, height: Int, filter: Boolean = true): Bitmap {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        if (!filter || (w >= src.width && h >= src.height)) return Bitmap.createScaledBitmap(src, w, h, filter)
        var current = src
        while (current.width / 2 >= w && current.height / 2 >= h) {
            val next = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
            if (current !== src) current.recycle()
            current = next
        }
        val result = Bitmap.createScaledBitmap(current, w, h, true)
        if (current !== src && current !== result) current.recycle()
        return result
    }
}
