package com.frameout.core.hdr

import android.graphics.Bitmap
import com.frameout.codecpack.FfmpegSignal

object FfmpegSignalLink {
    fun attach(path: String, bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled) return false
        val raw = FfmpegSignal.take(path, bitmap.width, bitmap.height) ?: return false
        if (!HdrSignal.supports(raw.primaries, raw.transfer)) return false
        HdrSignalRegistry.attach(bitmap, HdrSignal(raw.width, raw.height, raw.rgb, raw.primaries, raw.transfer))
        return true
    }
}
