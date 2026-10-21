package com.frameout.codecpack

import android.media.MediaCodecList
import android.media.MediaFormat

object DecoderSupport {
    fun decoderName(format: MediaFormat): String? = runCatching {
        val probe = probeFormat(format) ?: return null
        MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(probe)
    }.getOrNull()

    fun supports(format: MediaFormat): Boolean {
        val probe = probeFormat(format) ?: return true
        return try {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(probe) != null
        } catch (e: Exception) {
            true
        }
    }

    private fun probeFormat(format: MediaFormat): MediaFormat? {
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
        if (!format.containsKey(MediaFormat.KEY_WIDTH) || !format.containsKey(MediaFormat.KEY_HEIGHT)) return null
        val width = format.getInteger(MediaFormat.KEY_WIDTH)
        val height = format.getInteger(MediaFormat.KEY_HEIGHT)
        if (width <= 0 || height <= 0) return null
        val probe = MediaFormat.createVideoFormat(mime, width, height)
        if (format.containsKey(MediaFormat.KEY_PROFILE)) probe.setInteger(MediaFormat.KEY_PROFILE, format.getInteger(MediaFormat.KEY_PROFILE))
        if (format.containsKey(MediaFormat.KEY_LEVEL)) probe.setInteger(MediaFormat.KEY_LEVEL, format.getInteger(MediaFormat.KEY_LEVEL))
        return probe
    }
}
