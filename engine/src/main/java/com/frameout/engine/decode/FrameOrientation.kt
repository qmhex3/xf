package com.frameout.engine.decode

import android.graphics.Bitmap
import android.graphics.Matrix
import com.frameout.core.edit.GainmapOps
import kotlin.math.abs
import kotlin.math.roundToInt

object FrameOrientation {
    private const val ASPECT_TOLERANCE = 0.01f
    private const val ASPECT_LIMIT = 4f

    fun normalize(degrees: Int): Int = ((degrees % 360) + 360) % 360 / 90 * 90

    fun aspectOf(num: Int, den: Int): Float {
        if (num <= 0 || den <= 0) return 1f
        val value = num.toFloat() / den
        return if (abs(value - 1f) < ASPECT_TOLERANCE || value > ASPECT_LIMIT || value < 1f / ASPECT_LIMIT) 1f else value
    }

    fun outputSize(width: Int, height: Int, rotation: Int, aspect: Float): Pair<Int, Int> {
        val w = (width * aspect).roundToInt().coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        return if (rotation == 90 || rotation == 270) h to w else w to h
    }

    fun apply(source: Bitmap, rotation: Int, aspect: Float, flipH: Boolean = false): Bitmap {
        if (rotation == 0 && aspect == 1f && !flipH) return source
        val matrix = Matrix()
        if (aspect != 1f) matrix.postScale(aspect, 1f)
        if (flipH) matrix.postScale(-1f, 1f)
        if (rotation != 0) matrix.postRotate(rotation.toFloat())
        val out = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (out !== source) {
            if (aspect == 1f) GainmapOps.transform(source, out, rotation, flipH, false)
            source.recycle()
        }
        return out
    }
}
