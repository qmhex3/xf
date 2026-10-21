package com.frameout.app

import com.frameout.core.util.FrameStrings

internal fun deletionMessage(deleted: Int, failed: Int): String = when {
    failed == 0 && deleted > 0 -> FrameStrings.deleted()
    deleted > 0 -> FrameStrings.deletedPartial(deleted, failed)
    else -> FrameStrings.deleteFailed()
}
