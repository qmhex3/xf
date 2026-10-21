package com.frameout.engine.work

import com.frameout.core.util.FrameStrings
import com.frameout.engine.extract.ExtractProgress

internal fun extractStatusTitle(status: ExtractProgress.Status): String = when (status) {
    ExtractProgress.Status.Indexing -> FrameStrings.indexing()
    ExtractProgress.Status.Extracting -> FrameStrings.extracting()
    ExtractProgress.Status.Completed -> FrameStrings.completed()
    ExtractProgress.Status.Failed -> FrameStrings.failed()
    ExtractProgress.Status.Cancelled -> FrameStrings.cancel()
    ExtractProgress.Status.Paused -> FrameStrings.paused()
    else -> FrameStrings.working()
}
