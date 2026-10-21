package com.frameout.core.storage

import android.content.Context
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings

object SaveFailure {
    fun message(context: Context, fallback: String): String {
        val tree = AppPrefs(context).saveFolderUri.value.ifBlank { null }
        return if (StorageManager(context).hasDestinationRoom(tree, 0L)) fallback else FrameStrings.noSpace()
    }
}
