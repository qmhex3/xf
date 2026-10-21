package com.frameout.engine.extract

sealed class ExtractMode {
    data object EveryFrame : ExtractMode()
    data class EveryNFrames(val n: Int) : ExtractMode()
    data class EveryNMillis(val millis: Long) : ExtractMode()
    data object KeyframesOnly : ExtractMode()
    data class FixedCount(val count: Int) : ExtractMode()
    data object SceneChange : ExtractMode()
    data object Slides : ExtractMode()
}

data class ExtractRequest(
    val projectId: Long,
    val uri: String,
    val startMs: Long,
    val endMs: Long,
    val mode: ExtractMode,
    val autoSave: Boolean,
    val saveFormat: String = "jpg",
    val jpegQuality: Int = 92,
    val skipBlurry: Boolean = false,
    val skipDuplicates: Boolean = false,
    val peopleOnly: Boolean = false,
    val peopleBody: Boolean = true
) {
    fun newGate(): com.frameout.core.quality.ExtractQualityGate? =
        if (skipBlurry || skipDuplicates || peopleOnly) com.frameout.core.quality.ExtractQualityGate(skipBlurry, skipDuplicates, peopleOnly, peopleBody) { com.frameout.core.quality.SkipStats.record(projectId, it) } else null
}

data class ExtractProgress(
    val projectId: Long,
    val status: Status,
    val done: Int,
    val total: Int,
    val elapsedMs: Long,
    val currentPtsUs: Long,
    val message: String = ""
) {
    companion object {
        const val MESSAGE_LOW_SPACE = "low_space"
        const val MESSAGE_PAUSED_SYSTEM = "paused_system"
        const val MESSAGE_FAILURE_PREFIX = "failure:"

        fun failureMessage(reason: com.frameout.codecpack.MediaFailure): String = MESSAGE_FAILURE_PREFIX + reason.name

        fun failureOf(message: String): com.frameout.codecpack.MediaFailure? =
            if (message.startsWith(MESSAGE_FAILURE_PREFIX)) com.frameout.codecpack.MediaFailure.fromName(message.removePrefix(MESSAGE_FAILURE_PREFIX)) else null
    }

    enum class Status {
        Idle,
        Indexing,
        Extracting,
        Paused,
        Completed,
        Failed,
        Cancelled
    }

    val fraction: Float
        get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
}
