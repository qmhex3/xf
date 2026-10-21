package com.frameout.engine.decode

import android.content.Context
import android.media.MediaExtractor
import android.net.Uri
import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureClassifier
import com.frameout.codecpack.MediaFailureException

internal object SystemSource {
    fun open(context: Context, uri: Uri): MediaExtractor {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            return extractor
        } catch (e: Exception) {
            runCatching { extractor.release() }
            val reason = MediaFailureClassifier.classify(e)
            throw MediaFailureException(if (reason == MediaFailure.Unknown) MediaFailure.UnsupportedContainer else reason, e.message, e)
        }
    }

    fun createDecoder(name: String?, mime: String): android.media.MediaCodec = try {
        if (name != null) android.media.MediaCodec.createByCodecName(name) else android.media.MediaCodec.createDecoderByType(mime)
    } catch (e: Exception) {
        throw MediaFailureException(MediaFailure.DecoderMissing, e.message, e)
    }

    fun failOpen(e: Exception): Exception {
        if (e is MediaFailureException || e is kotlinx.coroutines.CancellationException) return e
        val reason = MediaFailureClassifier.classify(e, true)
        return MediaFailureException(if (reason == MediaFailure.Unknown) MediaFailure.DecoderInitFailed else reason, e.message, e)
    }
}
