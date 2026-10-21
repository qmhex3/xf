package com.frameout.engine.decode

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.frameout.codecpack.FormatKind
import com.frameout.core.storage.AnimatedFrameLookup

object AnimatedSources {
    fun handles(kind: FormatKind): Boolean = AnimatedFrameLookup.handles(kind)

    fun create(context: Context, kind: FormatKind): AnimatedFrameSource? = when (kind) {
        FormatKind.Gif -> GifFrameDecoder(context)
        FormatKind.Apng -> ApngFrameDecoder(context)
        FormatKind.AnimatedWebp -> WebpFrameDecoder(context)
        else -> null
    }

    fun decodeAtPts(context: Context, uri: Uri, kind: FormatKind, ptsUs: Long): Bitmap? {
        val decoder = create(context, kind) ?: return null
        return try {
            decoder.open(uri)
            decoder.decodeNearest(ptsUs)?.second
        } finally {
            decoder.close()
        }
    }

    fun lookup(context: Context): AnimatedFrameLookup = object : AnimatedFrameLookup {
        override fun decodeAtPts(uri: Uri, kind: FormatKind, ptsUs: Long): Bitmap? =
            runCatching { decodeAtPts(context.applicationContext, uri, kind, ptsUs) }.getOrNull()
    }
}
