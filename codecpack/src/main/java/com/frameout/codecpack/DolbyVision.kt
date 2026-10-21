package com.frameout.codecpack

import android.media.MediaCodecInfo
import android.media.MediaFormat

object DolbyVision {
    private const val MIME = "video/dolby-vision"

    fun isProfile5(format: MediaFormat): Boolean {
        val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
        if (!mime.equals(MIME, ignoreCase = true)) return false
        if (!format.containsKey(MediaFormat.KEY_PROFILE)) return false
        return format.getInteger(MediaFormat.KEY_PROFILE) == MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheStn
    }
}
