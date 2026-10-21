package com.frameout.engine.edit

import com.frameout.core.util.logFailure
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.frameout.core.media.MotionPhotoExtractor
import java.io.File
import java.nio.ByteBuffer

internal class VideoSampleCopier(private val context: Context) {
    private companion object {
        const val MAX_ALIGN_SHIFT_US = 2_000_000L
    }

    private data class Tracks(val map: Map<Int, Int>, val video: Int, var buffer: ByteBuffer, val rotation: Int)

    fun copy(plan: ReassemblePlan, out: File): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            val source = MotionPhotoExtractor.resolveVideoSource(context, Uri.parse(plan.sourceUri))
            extractor.setDataSource(context, source, null)
            muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val tracks = addTracks(extractor, muxer, source) ?: return false
            muxer.setOrientationHint(tracks.rotation)
            val ranges = alignRangesToKeyframes(plan.keptRanges) { start -> keyframeAtOrBefore(extractor, tracks.video, start) } ?: return false
            muxer.start()
            copyRanges(extractor, muxer, tracks, ranges).also { ok -> if (ok) muxer?.stop() }
        } catch (e: Throwable) {
            logFailure("VideoSampleCopier", e)
            false
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun addTracks(extractor: MediaExtractor, muxer: MediaMuxer, source: Uri): Tracks? {
        val map = linkedMapOf<Int, Int>()
        var video = -1
        var rotation = -1
        var maxBytes = 4 * 1024 * 1024
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (!mime.startsWith("video/") && !mime.startsWith("audio/")) continue
            if (mime.startsWith("video/") && video < 0) {
                video = i
                if (format.containsKey(MediaFormat.KEY_ROTATION)) rotation = format.getInteger(MediaFormat.KEY_ROTATION)
            }
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) maxBytes = maxOf(maxBytes, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
            map[i] = muxer.addTrack(format)
        }
        if (map.isEmpty() || video < 0) return null
        val degrees = normalizeRotation(if (rotation >= 0) rotation else retrieverRotation(source))
        return Tracks(map, video, ByteBuffer.allocate(maxBytes.coerceIn(4 * 1024 * 1024, 64 * 1024 * 1024)), degrees)
    }

    private fun retrieverRotation(source: Uri): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, source)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } catch (e: Throwable) {
            logFailure("VideoSampleCopier", e)
            0
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun normalizeRotation(degrees: Int): Int {
        val wrapped = ((degrees % 360) + 360) % 360
        return if (wrapped % 90 == 0) wrapped else 0
    }

    private fun copyRanges(extractor: MediaExtractor, muxer: MediaMuxer, tracks: Tracks, ranges: List<KeptRange>): Boolean {
        val info = MediaCodec.BufferInfo()
        var nextPts = 0L
        for (range in ranges) {
            nextPts = copyRange(extractor, muxer, tracks, range, info, nextPts) ?: return false
        }
        return true
    }

    private fun copyRange(extractor: MediaExtractor, muxer: MediaMuxer, tracks: Tracks, range: KeptRange, info: MediaCodec.BufferInfo, nextPts: Long): Long? {
        if (!seekToVideoStart(extractor, tracks.video, range.startUs)) return null
        tracks.map.keys.forEach(extractor::selectTrack)
        extractor.seekTo(range.startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        if (extractor.sampleTrackIndex < 0) return null
        return try { writeRangeSamples(extractor, muxer, tracks, range, info, nextPts) } finally { tracks.map.keys.forEach { runCatching { extractor.unselectTrack(it) } } }
    }

    private fun keyframeAtOrBefore(extractor: MediaExtractor, videoTrack: Int, startUs: Long): Long? {
        if (!seekToVideoStart(extractor, videoTrack, startUs)) return null
        val pts = extractor.sampleTime
        runCatching { extractor.unselectTrack(videoTrack) }
        return pts.takeIf { startUs - it <= MAX_ALIGN_SHIFT_US }
    }

    private fun seekToVideoStart(extractor: MediaExtractor, videoTrack: Int, startUs: Long): Boolean {
        tracksReset(extractor, videoTrack)
        extractor.selectTrack(videoTrack)
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val pts = extractor.sampleTime
        return extractor.sampleTrackIndex == videoTrack && pts >= 0L && pts <= startUs
    }

    private fun tracksReset(extractor: MediaExtractor, videoTrack: Int) {
        runCatching { extractor.unselectTrack(videoTrack) }
        if (extractor.trackCount > 0) {
            for (i in 0 until extractor.trackCount) if (i != videoTrack) runCatching { extractor.unselectTrack(i) }
        }
    }

    private fun writeRangeSamples(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        tracks: Tracks,
        range: KeptRange,
        info: MediaCodec.BufferInfo,
        nextPts: Long
    ): Long? {
        val gate = SampleGate(range, nextPts, tracks.video)
        while (true) {
            val size = extractor.sampleSize
            if (size < 0) break
            val src = extractor.sampleTrackIndex
            val sync = extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
            val decision = gate.decide(src, extractor.sampleTime, sync)
            if (decision is SampleDecision.Stop) break
            if (decision is SampleDecision.Write && !writeSample(extractor, muxer, tracks, info, src, sync, decision.outPtsUs)) break
            if (!extractor.advance()) break
        }
        return gate.maxOutUs + 1L
    }

    private fun writeSample(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        tracks: Tracks,
        info: MediaCodec.BufferInfo,
        src: Int,
        sync: Boolean,
        outPts: Long
    ): Boolean {
        tracks.buffer = ensureBuffer(tracks.buffer, extractor.sampleSize)
        val read = extractor.readSampleData(tracks.buffer, 0)
        if (read < 0) return false
        info.offset = 0
        info.size = read
        info.presentationTimeUs = outPts
        info.flags = if (src == tracks.video && sync) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
        muxer.writeSampleData(tracks.map.getValue(src), tracks.buffer, info)
        return true
    }

    private fun ensureBuffer(current: ByteBuffer, sampleSize: Long): ByteBuffer {
        if (sampleSize <= current.capacity()) return current.apply { clear() }
        if (sampleSize > 128L * 1024L * 1024L) error("Sample exceeds 128MB")
        return ByteBuffer.allocate(sampleSize.toInt().coerceAtLeast(current.capacity() * 2))
    }
}
