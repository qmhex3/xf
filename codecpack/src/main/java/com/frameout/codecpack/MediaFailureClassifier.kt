package com.frameout.codecpack

import android.media.DeniedByServerException
import android.media.MediaCodec
import android.media.MediaDrm
import android.media.NotProvisionedException
import android.media.UnsupportedSchemeException
import java.io.FileNotFoundException
import java.io.IOException

object MediaFailureClassifier {
    private const val MAX_CAUSE_DEPTH = 8

    fun classify(error: Throwable, opening: Boolean = false): MediaFailure {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            val found = single(current, opening)
            if (found != MediaFailure.Unknown) return found
            current = current.cause
            depth++
        }
        return MediaFailure.Unknown
    }

    private fun single(error: Throwable, opening: Boolean): MediaFailure = when (error) {
        is MediaFailureException -> error.reason
        is FileNotFoundException -> MediaFailure.SourceMissing
        is SecurityException -> MediaFailure.PermissionDenied
        is OutOfMemoryError -> MediaFailure.OutOfMemory
        is MediaCodec.CryptoException -> MediaFailure.Drm
        is NotProvisionedException, is DeniedByServerException, is UnsupportedSchemeException -> MediaFailure.Drm
        is MediaDrm.MediaDrmStateException -> MediaFailure.Drm
        is MediaCodec.CodecException -> codec(error, opening)
        is RuntimeIntegrityException -> MediaFailure.Integrity
        is UnsupportedOperationException -> MediaFailure.UnsupportedFeature
        is IOException -> MediaFailure.IoError
        else -> MediaFailure.Unknown
    }

    private fun codec(error: MediaCodec.CodecException, opening: Boolean): MediaFailure {
        val resource = error.errorCode == MediaCodec.CodecException.ERROR_INSUFFICIENT_RESOURCE ||
            error.errorCode == MediaCodec.CodecException.ERROR_RECLAIMED
        return if (opening || resource) MediaFailure.DecoderInitFailed else MediaFailure.CorruptData
    }
}
