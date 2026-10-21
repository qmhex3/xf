package com.frameout.engine.decode

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaCodecInfo
import android.os.Build
import com.frameout.core.util.DiagLog

internal const val COLOR_FORMAT_P010 = 54

internal fun wantsP010(format: MediaFormat): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    if (HdrDetector.isHdr(format)) return true
    if (!format.containsKey(MediaFormat.KEY_PROFILE)) return false
    val profile = format.getInteger(MediaFormat.KEY_PROFILE)
    return when (format.getString(MediaFormat.KEY_MIME)) {
        MediaFormat.MIMETYPE_VIDEO_HEVC -> profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
            profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 ||
            profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus
        MediaFormat.MIMETYPE_VIDEO_VP9 -> profile == MediaCodecInfo.CodecProfileLevel.VP9Profile2 ||
            profile == MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR ||
            profile == MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus
        MediaFormat.MIMETYPE_VIDEO_AV1 -> profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10 ||
            profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10 ||
            profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus
        else -> false
    }
}

internal fun supportsP010(dec: MediaCodec, mime: String): Boolean = runCatching {
    dec.codecInfo.getCapabilitiesForType(mime).colorFormats.contains(COLOR_FORMAT_P010)
}.getOrDefault(false)

internal fun videoOrdinal(extractor: MediaExtractor, track: Int): Int {
    var count = 0
    for (i in 0 until track) {
        val mime = runCatching { extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) }.getOrNull()
        if (mime?.startsWith("video/") == true) count++
    }
    return count
}

internal fun logSlowPath(frames: Int, ptsUs: Long, format: MediaFormat?) {
    DiagLog.append("decode", "slow path: $frames frames needed MediaMetadataRetriever fallback (last pts=$ptsUs, hdr=${format?.let { HdrDetector.isHdr(it) }}, format=$format)")
}
