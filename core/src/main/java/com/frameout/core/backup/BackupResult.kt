package com.frameout.core.backup

import com.frameout.core.util.FrameStrings

enum class BackupResult {
    Saved, Imported, Duplicate, Invalid, Newer, NoSpace, Failed;

    fun message(): String = when (this) {
        Saved -> FrameStrings.backupSaved()
        Imported, Duplicate -> FrameStrings.backupImported()
        Invalid -> FrameStrings.backupInvalid()
        Newer -> FrameStrings.backupNewer()
        NoSpace -> FrameStrings.noSpace()
        Failed -> FrameStrings.saveFailed()
    }
}
