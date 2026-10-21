package com.frameout.engine.decode

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

object HdrDetector {
    fun isHdr(context: Context, uri: Uri): Boolean = runCatching {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            (0 until extractor.trackCount).map { extractor.getTrackFormat(it) }.firstOrNull { isVideo(it) }?.let { isHdr(it) } ?: false
        } finally {
            extractor.release()
        }
    }.getOrDefault(false)

    fun isHdr(format: MediaFormat): Boolean {
        if (ColorParams.from(format).hdr) return true
        val vui = runCatching { VuiColor.read(format) ?: Av1Color.read(format) }.getOrNull() ?: return false
        return vui.hasDescription && (vui.transfer == TRANSFER_PQ || vui.transfer == TRANSFER_HLG)
    }

    private const val TRANSFER_PQ = 16
    private const val TRANSFER_HLG = 18

    private fun isVideo(format: MediaFormat): Boolean = format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
}
