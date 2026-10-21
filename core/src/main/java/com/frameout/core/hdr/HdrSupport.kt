package com.frameout.core.hdr

import android.content.Context
import android.os.Build
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.storage.ImageFormat

object HdrSupport {
    @Volatile
    var source: HdrFrameSource? = null

    fun supported(): Boolean = Build.VERSION.SDK_INT >= 34

    fun exact(context: Context): Boolean = supported() && ImageFormat.fromKey(AppPrefs(context).saveFormat.value) == ImageFormat.PngHdr16

    fun enabled(context: Context): Boolean = supported() && ImageFormat.fromKey(AppPrefs(context).saveFormat.value).hdr
}
