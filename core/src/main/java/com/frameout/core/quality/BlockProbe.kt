package com.frameout.core.quality

import android.graphics.Bitmap
import java.io.File
import kotlin.math.min

object BlockProbe {
    const val CROP_WIDTH = 960
    const val CROP_HEIGHT = 540

    fun factor(bitmap: Bitmap): Double = runCatching { measure(bitmap) }.getOrDefault(1.0)

    private fun measure(bitmap: Bitmap): Double {
        if (bitmap.isRecycled) return 1.0
        val w = min(bitmap.width, CROP_WIDTH)
        val h = min(bitmap.height, CROP_HEIGHT)
        if (w < BlockGrid.MIN_SIDE || h < BlockGrid.MIN_SIDE) return 1.0
        val luma = Sharpness.lumaRegion(bitmap, (bitmap.width - w) / 2, (bitmap.height - h) / 2, w, h)
        return BlockAware.assess(luma, w, h).factor
    }

    fun record(projectDir: File, index: Int, bitmap: Bitmap) {
        runCatching { BlockLedger.record(projectDir, index, factor(bitmap)) }
    }
}
