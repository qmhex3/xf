package com.frameout.engine.extract

import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureException

class FrameDecodeException(
    val sampleIndex: Int,
    message: String,
    reason: MediaFailure = MediaFailure.CorruptData,
    cause: Throwable? = null
) : MediaFailureException(reason, message, cause)
