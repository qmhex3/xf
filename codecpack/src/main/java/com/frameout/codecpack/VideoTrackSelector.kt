package com.frameout.codecpack

import android.media.MediaExtractor
import android.media.MediaFormat

data class TrackInfo(val index: Int, val mime: String, val width: Int, val height: Int, val durationUs: Long)

object VideoTrackSelector {
    private val stills = setOf("video/mjpeg", "video/x-mjpeg", "video/jpeg", "video/png", "video/bmp")

    fun choose(tracks: List<TrackInfo>): Int {
        val videos = tracks.filter { it.mime.startsWith("video/") }
        if (videos.isEmpty()) return -1
        return videos.sortedWith(
            compareBy<TrackInfo> { it.mime in stills }
                .thenByDescending { it.durationUs }
                .thenByDescending { it.width.toLong() * it.height.toLong() }
                .thenBy { it.index }
        ).first().index
    }

    fun select(extractor: MediaExtractor): Int = choose((0 until extractor.trackCount).map { info(extractor, it) })

    fun info(extractor: MediaExtractor, index: Int): TrackInfo {
        val f = extractor.getTrackFormat(index)
        return TrackInfo(
            index,
            f.getString(MediaFormat.KEY_MIME).orEmpty(),
            if (f.containsKey(MediaFormat.KEY_WIDTH)) f.getInteger(MediaFormat.KEY_WIDTH) else 0,
            if (f.containsKey(MediaFormat.KEY_HEIGHT)) f.getInteger(MediaFormat.KEY_HEIGHT) else 0,
            if (f.containsKey(MediaFormat.KEY_DURATION)) f.getLong(MediaFormat.KEY_DURATION) else 0L
        )
    }
}
