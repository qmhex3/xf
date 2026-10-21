package com.frameout.engine.decode

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.frameout.core.media.VideoThumbLookup

object VideoThumbSources {
    fun lookup(context: Context): VideoThumbLookup = object : VideoThumbLookup {
        override fun firstFrame(uri: Uri): Bitmap? = runCatching {
            val decoder = FrameDecoder(context.applicationContext)
            try {
                decoder.open(uri)
                decoder.decodeNextBitmap()?.second
            } finally {
                decoder.close()
            }
        }.getOrNull()
    }
}
