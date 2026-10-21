package com.frameout.codecpack

enum class MediaFailure(val code: Int) {
    None(0),
    Unknown(1),
    SourceMissing(2),
    PermissionDenied(3),
    IoError(4),
    CorruptData(5),
    NoVideoStream(6),
    DecoderMissing(7),
    DecoderInitFailed(8),
    Drm(9),
    UnsupportedFeature(10),
    OutOfMemory(11),
    Internal(12),
    UnsupportedContainer(13),
    CodecPackMissing(100),
    NotAnimated(102),
    Integrity(103);

    val fatal: Boolean get() = this in FATAL

    companion object {
        private val FATAL = setOf(
            SourceMissing, PermissionDenied, NoVideoStream, DecoderMissing, DecoderInitFailed, Drm,
            UnsupportedFeature, UnsupportedContainer, CodecPackMissing, NotAnimated, OutOfMemory, Integrity
        )

        private val byCode: Map<Int, MediaFailure> = entries.associateBy { it.code }

        fun fromCode(code: Int): MediaFailure = byCode[code] ?: Unknown

        fun fromName(name: String): MediaFailure? = entries.firstOrNull { it.name == name }
    }
}

open class MediaFailureException(
    val reason: MediaFailure,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message ?: reason.name, cause)
