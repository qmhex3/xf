package com.frameout.engine.extract

import java.io.IOException

class LowSpaceException : Exception(ExtractProgress.MESSAGE_LOW_SPACE)

fun Throwable.isDiskFull(): Boolean = generateSequence(this) { it.cause }.take(8).any {
    it is LowSpaceException || (it is IOException && it.message.orEmpty().lowercase().let { m -> m.contains("enospc") || m.contains("no space") || m.contains("not enough space") })
}
