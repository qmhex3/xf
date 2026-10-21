package com.frameout.engine.player

import android.content.Context
import android.media.MediaExtractor
import android.net.Uri
import com.frameout.codecpack.VideoTrackSelector
import com.frameout.core.util.logged

object ExactPts {
    const val SLACK_US = 999L
    private const val REORDER_WINDOW = 16

    fun limitUs(positionMs: Long): Long = positionMs.coerceAtLeast(0L) * 1000L + SLACK_US

    fun pickDisplayed(samples: List<Long>, limitUs: Long): Long? = samples.filter { it <= limitUs }.maxOrNull()

    fun displayedAt(context: Context, uri: Uri, positionMs: Long): Long? = runCatching {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val track = VideoTrackSelector.select(extractor)
            if (track < 0) return null
            extractor.selectTrack(track)
            val limit = limitUs(positionMs)
            extractor.seekTo(limit, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            pickDisplayed(collect(extractor, limit), limit)
        } finally {
            extractor.release()
        }
    }.logged("ExactPts").getOrNull()

    private fun collect(extractor: MediaExtractor, limitUs: Long): List<Long> {
        val seen = ArrayList<Long>()
        var past = 0
        while (past <= REORDER_WINDOW) {
            val pts = extractor.sampleTime
            if (pts < 0L) break
            if (pts <= limitUs) seen.add(pts) else past++
            if (!extractor.advance()) break
        }
        return seen
    }
}
