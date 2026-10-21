package com.frameout.engine.decode

import android.media.MediaFormat

internal object VideoFormatInfo {
    fun rotation(format: MediaFormat): Int =
        FrameOrientation.normalize(if (format.containsKey(MediaFormat.KEY_ROTATION)) format.getInteger(MediaFormat.KEY_ROTATION) else 0)

    fun pixelAspect(format: MediaFormat): Float {
        if (android.os.Build.VERSION.SDK_INT < 30) return 1f
        val w = if (format.containsKey(MediaFormat.KEY_PIXEL_ASPECT_RATIO_WIDTH)) format.getInteger(MediaFormat.KEY_PIXEL_ASPECT_RATIO_WIDTH) else 1
        val h = if (format.containsKey(MediaFormat.KEY_PIXEL_ASPECT_RATIO_HEIGHT)) format.getInteger(MediaFormat.KEY_PIXEL_ASPECT_RATIO_HEIGHT) else 1
        return FrameOrientation.aspectOf(w, h)
    }
}
