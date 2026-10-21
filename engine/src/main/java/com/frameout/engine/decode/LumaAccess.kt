package com.frameout.engine.decode

import android.graphics.ImageFormat
import android.media.MediaCodec
import android.media.MediaFormat
import com.frameout.core.quality.LumaGrid

internal object LumaAccess {
    fun <T> read(
        dec: MediaCodec,
        index: Int,
        inputFormat: MediaFormat?,
        rotation: Int,
        flipH: Boolean,
        block: (Int, Int, LumaGrid) -> T
    ): T? {
        val image = runCatching { dec.getOutputImage(index) }.getOrNull() ?: return null
        try {
            val wide = when (image.format) {
                ImageFormat.YUV_420_888 -> false
                COLOR_FORMAT_P010 -> true
                else -> return null
            }
            val plane = image.planes.getOrNull(0) ?: return null
            val crop = image.cropRect
            if (crop.width() <= 0 || crop.height() <= 0) return null
            val limited = ColorParams.from(runCatching { dec.getOutputFormat(index) }.getOrNull(), inputFormat).limited
            val turn = FrameOrientation.normalize(rotation)
            val grid = LumaPlaneGrid.of(plane, crop, turn, flipH, wide, limited)
            return if (turn == 90 || turn == 270) block(crop.height(), crop.width(), grid) else block(crop.width(), crop.height(), grid)
        } finally {
            image.close()
        }
    }
}
