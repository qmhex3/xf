package com.frameout.core.hdr

import android.graphics.Bitmap
import android.net.Uri

fun interface HdrFrameSource {
    fun fetch(source: Uri, ptsUs: Long): Bitmap?

    fun fetchExact(source: Uri, ptsUs: Long): Bitmap? = fetch(source, ptsUs)

    fun isHdrSource(source: Uri): Boolean = true
}
