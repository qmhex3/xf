package com.frameout.core.storage

import android.media.MediaCodecInfo
import android.media.MediaCodecList

internal object ModernCodecs {
    private val encoders: List<MediaCodecInfo> by lazy {
        runCatching { MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { it.isEncoder } }.getOrDefault(emptyList())
    }

    private fun has(vararg mimes: String): Boolean =
        encoders.any { info -> info.supportedTypes.any { type -> mimes.any { it.equals(type, ignoreCase = true) } } }

    fun heic(sdk: Int): Boolean = sdk >= MIN_SDK && has("image/vnd.android.heic", "video/hevc")

    fun avif(sdk: Int): Boolean = sdk >= MIN_SDK && has("image/avif", "video/av01")

    const val MIN_SDK = 29
}
