package com.frameout.app.frames

import com.frameout.core.util.FrameStrings

private val FAILURE_WORDS = listOf("fail", "couldn't", "unable", "فشل", "تعذّر", "تعذر", "لا توجد مساحة")

private fun failureTexts(): Set<String> = setOf(
    FrameStrings.deleteFailed(),
    FrameStrings.noSpace(),
    FrameStrings.saveFailed(),
    FrameStrings.reassembleFailed(),
    FrameStrings.savedMissing(),
    FrameStrings.noSourceFrame(),
    FrameStrings.sourceGone(),
    FrameStrings.nothingSaved(),
    FrameStrings.exportSourceMissing(),
    FrameStrings.gifNeedsTwo(),
    FrameStrings.compareNeedsTwo(),
    FrameStrings.contactNeedsOne(),
    FrameStrings.contactTooMany(),
    FrameStrings.backupInvalid(),
    FrameStrings.backupNewer()
)

internal fun isStatusError(message: String): Boolean =
    FAILURE_WORDS.any { message.contains(it, ignoreCase = true) } || message in failureTexts()
