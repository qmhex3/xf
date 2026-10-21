package com.frameout.core.storage

import android.net.Uri

sealed interface CaptureSave {
    data class Saved(val uri: Uri, val mime: String, val name: String, val metadataWritten: Boolean) : CaptureSave
    data class Failed(val reason: Reason) : CaptureSave

    enum class Reason { NoSpace, Unwritable, Empty }
}
