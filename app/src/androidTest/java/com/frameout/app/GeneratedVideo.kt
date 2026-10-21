package com.frameout.app

import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import java.io.File

class VideoSpec(
    val frames: Int,
    val ptsUs: (Int) -> Long,
    val maxBFrames: Int = 0,
    val width: Int = 320,
    val height: Int = 240,
    val mime: String = "video/avc",
    val rotation: Int = 0
) {
    val container: Int
        get() = if (mime == "video/x-vnd.on2.vp8" || mime == "video/x-vnd.on2.vp9") MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM else MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4

    val extension: String
        get() = if (container == MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM) "webm" else "mp4"
}

object GeneratedVideo {
    private const val TIMEOUT_US = 10_000L
    private const val DEADLINE_NS = 120_000_000_000L

    fun write(file: File, spec: VideoSpec) {
        val codec = MediaCodec.createEncoderByType(spec.mime)
        val muxer = MediaMuxer(file.absolutePath, spec.container).also { it.setOrientationHint(spec.rotation) }
        try {
            codec.configure(format(spec), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            pump(codec, muxer, spec)
        } finally {
            runCatching { codec.stop() }
            codec.release()
            runCatching { muxer.stop() }
            muxer.release()
        }
    }

    fun sampleTimes(file: File): List<Long> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            extractor.selectTrack(0)
            val times = ArrayList<Long>()
            while (extractor.sampleTime >= 0) {
                times += extractor.sampleTime
                if (!extractor.advance()) break
            }
            return times
        } finally {
            extractor.release()
        }
    }

    fun isReordered(times: List<Long>): Boolean = times.zipWithNext().any { (a, b) -> b < a }

    private fun format(spec: VideoSpec): MediaFormat =
        MediaFormat.createVideoFormat(spec.mime, spec.width, spec.height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, 1_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            if (spec.maxBFrames > 0 && spec.mime == "video/avc" && Build.VERSION.SDK_INT >= 29) {
                setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
                setInteger(MediaFormat.KEY_MAX_B_FRAMES, spec.maxBFrames)
            }
        }

    private fun pump(codec: MediaCodec, muxer: MediaMuxer, spec: VideoSpec) {
        val info = MediaCodec.BufferInfo()
        val deadline = System.nanoTime() + DEADLINE_NS
        var fed = 0
        var track = -1
        var done = false
        while (!done) {
            check(System.nanoTime() < deadline) { "encoder timed out" }
            if (fed <= spec.frames) fed += feed(codec, spec, fed)
            val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                track = muxer.addTrack(codec.outputFormat)
                muxer.start()
            } else if (index >= 0) {
                done = drain(codec, muxer, track, index, info)
            }
        }
    }

    private fun feed(codec: MediaCodec, spec: VideoSpec, frame: Int): Int {
        val index = codec.dequeueInputBuffer(TIMEOUT_US)
        if (index < 0) return 0
        if (frame == spec.frames) {
            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return 1
        }
        val image = checkNotNull(codec.getInputImage(index))
        paint(image, frame, spec)
        image.close()
        codec.queueInputBuffer(index, 0, spec.width * spec.height * 3 / 2, spec.ptsUs(frame), 0)
        return 1
    }

    private fun drain(codec: MediaCodec, muxer: MediaMuxer, track: Int, index: Int, info: MediaCodec.BufferInfo): Boolean {
        val config = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
        if (!config && info.size > 0 && track >= 0) muxer.writeSampleData(track, checkNotNull(codec.getOutputBuffer(index)), info)
        codec.releaseOutputBuffer(index, false)
        return info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
    }

    private fun paint(image: Image, frame: Int, spec: VideoSpec) {
        val base = 40 + frame * 7 % 180
        val left = frame * 4 % (spec.width - 48)
        fill(image.planes[0], spec.width, spec.height) { x, y -> if (x in left until left + 48 && y in 40 until 88) 235 else base }
        fill(image.planes[1], spec.width / 2, spec.height / 2) { _, _ -> 128 }
        fill(image.planes[2], spec.width / 2, spec.height / 2) { _, _ -> 128 }
    }

    private fun fill(plane: Image.Plane, width: Int, height: Int, value: (Int, Int) -> Int) {
        val buffer = plane.buffer
        for (y in 0 until height) {
            for (x in 0 until width) buffer.put(y * plane.rowStride + x * plane.pixelStride, value(x, y).toByte())
        }
    }
}
