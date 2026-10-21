package com.frameout.core.util

import com.frameout.codecpack.MediaFailure
import com.frameout.core.R

object FailureStrings {
    fun of(reason: MediaFailure): String = StringSource.get(resource(reason))

    private fun resource(reason: MediaFailure): Int = when (reason) {
        MediaFailure.SourceMissing -> R.string.fail_source_missing
        MediaFailure.PermissionDenied -> R.string.fail_permission
        MediaFailure.IoError -> R.string.fail_io
        MediaFailure.CorruptData -> R.string.fail_corrupt
        MediaFailure.NoVideoStream -> R.string.fail_no_video
        MediaFailure.DecoderMissing -> R.string.fail_decoder_missing
        MediaFailure.DecoderInitFailed -> R.string.fail_decoder_init
        MediaFailure.Drm -> R.string.fail_drm
        MediaFailure.UnsupportedFeature -> R.string.fail_unsupported_feature
        MediaFailure.OutOfMemory -> R.string.fail_oom
        MediaFailure.Internal -> R.string.fail_internal
        MediaFailure.UnsupportedContainer -> R.string.fail_container
        MediaFailure.CodecPackMissing -> R.string.fail_codec_pack
        MediaFailure.NotAnimated -> R.string.fail_not_animated
        MediaFailure.Integrity -> R.string.fail_integrity
        MediaFailure.None, MediaFailure.Unknown -> R.string.failed
    }
}
