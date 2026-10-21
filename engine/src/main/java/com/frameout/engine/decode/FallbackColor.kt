package com.frameout.engine.decode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.os.Build

internal object FallbackColor {
    fun normalize(bitmap: Bitmap): Bitmap {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return bitmap
        val srgb = ColorSpace.get(ColorSpace.Named.SRGB)
        val plain = bitmap.config == Bitmap.Config.ARGB_8888 && (bitmap.colorSpace ?: srgb) == srgb
        if (plain) return bitmap
        val readable = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return bitmap else bitmap
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888, true, srgb)
        Canvas(out).drawBitmap(readable, 0f, 0f, null)
        if (readable !== bitmap) readable.recycle()
        bitmap.recycle()
        return out
    }
}
