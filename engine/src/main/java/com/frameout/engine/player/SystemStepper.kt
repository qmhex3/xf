package com.frameout.engine.player

import com.frameout.core.util.logged
import android.content.Context
import android.media.MediaExtractor
import android.net.Uri
import com.frameout.codecpack.VideoTrackSelector

object SystemStepper {
    private const val REORDER_WINDOW = 16
    private const val MAX_SEEKS = 40

    fun step(context: Context, uri: Uri, currentMs: Long, forward: Boolean, count: Int): Long? = runCatching {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val track = VideoTrackSelector.select(extractor)
            if (track < 0) return null
            extractor.selectTrack(track)
            val target = currentMs.coerceAtLeast(0L) * 1000L
            if (forward) forwardStep(extractor, target, count) else backwardStep(extractor, target, count)
        } finally {
            extractor.release()
        }
    }.logged("SystemStepper").getOrNull()

    fun pickNext(after: List<Long>, count: Int): Long? =
        after.sorted().let { if (it.isEmpty()) null else it[minOf(count, it.size) - 1] / 1000L }

    fun pickPrevious(before: List<Long>, count: Int): Long? =
        before.sorted().let { if (it.isEmpty()) null else it[maxOf(0, it.size - count)] / 1000L }

    private fun forwardStep(extractor: MediaExtractor, targetUs: Long, count: Int): Long? {
        extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val after = ArrayList<Long>()
        while (after.size < count + REORDER_WINDOW) {
            val pts = extractor.sampleTime
            if (pts < 0L) break
            if (pts > targetUs) after.add(pts)
            if (!extractor.advance()) break
        }
        return pickNext(after, count)
    }

    private fun backwardStep(extractor: MediaExtractor, targetUs: Long, count: Int): Long? {
        var seekTo = targetUs
        var best: List<Long> = emptyList()
        repeat(MAX_SEEKS) {
            extractor.seekTo(seekTo, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val start = extractor.sampleTime
            best = collectBefore(extractor, targetUs)
            if (best.size >= count || start <= 0L) return pickPrevious(best, count)
            seekTo = start - 1L
        }
        return pickPrevious(best, count)
    }

    private fun collectBefore(extractor: MediaExtractor, targetUs: Long): List<Long> {
        val before = ArrayList<Long>()
        var past = 0
        while (past <= REORDER_WINDOW) {
            val pts = extractor.sampleTime
            if (pts < 0L) break
            if (pts < targetUs) before.add(pts) else past++
            if (!extractor.advance()) break
        }
        return before
    }
}
