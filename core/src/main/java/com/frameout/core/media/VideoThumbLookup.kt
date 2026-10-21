package com.frameout.core.media

import android.graphics.Bitmap
import android.net.Uri

interface VideoThumbLookup {
    fun firstFrame(uri: Uri): Bitmap?

    companion object {
        @Volatile
        var provider: VideoThumbLookup? = null
    }
}
