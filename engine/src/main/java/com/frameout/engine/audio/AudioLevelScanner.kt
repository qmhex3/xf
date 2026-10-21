package com.frameout.engine.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class AudioLevelScanner(private val context: Context) {
    suspend fun scan(uri: Uri): FloatArray = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull { mimeOf(extractor, it).startsWith("audio/") }
            if (track == null) FloatArray(0) else decode(extractor, track)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            FloatArray(0)
        } finally {
            extractor.release()
        }
    }

    private fun mimeOf(extractor: MediaExtractor, track: Int): String =
        extractor.getTrackFormat(track).getString(MediaFormat.KEY_MIME).orEmpty()

    private suspend fun decode(extractor: MediaExtractor, track: Int): FloatArray {
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(mimeOf(extractor, track))
        val acc = LevelAccumulator(WINDOW_MS)
        configureAccumulator(acc, format)
        try {
            codec.configure(format, null, null, 0)
            codec.start()
            pump(codec, extractor, acc)
        } finally {
            runCatching { codec.stop() }
            codec.release()
        }
        return acc.finish()
    }

    private suspend fun pump(codec: MediaCodec, extractor: MediaExtractor, acc: LevelAccumulator) {
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var idle = 0
        while (idle < MAX_IDLE) {
            currentCoroutineContext().ensureActive()
            if (!inputDone) inputDone = queueInput(codec, extractor)
            val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            idle = if (index == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) idle + 1 else 0
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) configureAccumulator(acc, codec.outputFormat)
            if (index >= 0 && drain(codec, index, info, acc)) return
        }
    }

    private fun queueInput(codec: MediaCodec, extractor: MediaExtractor): Boolean {
        val index = codec.dequeueInputBuffer(TIMEOUT_US)
        if (index < 0) return false
        val buffer = codec.getInputBuffer(index) ?: return false
        val size = extractor.readSampleData(buffer, 0)
        if (size < 0) {
            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return true
        }
        codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
        extractor.advance()
        return false
    }

    private fun drain(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo, acc: LevelAccumulator): Boolean {
        val buffer = codec.getOutputBuffer(index)
        if (buffer != null && info.size > 0) acc.feed(buffer, info.offset, info.size)
        codec.releaseOutputBuffer(index, false)
        return info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
    }

    private fun configureAccumulator(acc: LevelAccumulator, format: MediaFormat) {
        val encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) format.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
        if (encoding != AudioFormat.ENCODING_PCM_16BIT && encoding != AudioFormat.ENCODING_PCM_FLOAT) return
        if (!format.containsKey(MediaFormat.KEY_SAMPLE_RATE) || !format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) return
        acc.configure(format.getInteger(MediaFormat.KEY_SAMPLE_RATE), format.getInteger(MediaFormat.KEY_CHANNEL_COUNT), encoding == AudioFormat.ENCODING_PCM_FLOAT)
    }

    companion object {
        const val WINDOW_MS = 100L
        private const val TIMEOUT_US = 10_000L
        private const val MAX_IDLE = 300
    }
}
