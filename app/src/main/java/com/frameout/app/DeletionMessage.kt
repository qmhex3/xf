package com.frameout.app

import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SavedStrings

internal fun deletionMessage(deleted: Int, failed: Int, saved: Boolean = false): String = when {
    failed == 0 && deleted > 0 -> if (saved) SavedStrings.deletedN(deleted) else FrameStrings.projectsDeleted(deleted)
    deleted > 0 -> FrameStrings.deletedPartial(deleted, failed)
    else -> FrameStrings.deleteFailed()
}
