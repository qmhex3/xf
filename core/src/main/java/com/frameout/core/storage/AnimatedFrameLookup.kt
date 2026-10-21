package com.frameout.core.storage

import android.graphics.Bitmap
import android.net.Uri
import com.frameout.codecpack.FormatKind

interface AnimatedFrameLookup {
    fun decodeAtPts(uri: Uri, kind: FormatKind, ptsUs: Long): Bitmap?

    companion object {
        @Volatile
        var provider: AnimatedFrameLookup? = null

        fun handles(kind: FormatKind): Boolean = kind == FormatKind.Gif || kind == FormatKind.Apng || kind == FormatKind.AnimatedWebp
    }
}
