package com.frameout.engine.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build

class StillImageSource(private val context: Context) : AnimatedFrameSource {
    private var bitmap: Bitmap? = null

    override fun open(uri: Uri) {
        close()
        if (Build.VERSION.SDK_INT < FIRST_AVIF_API) error("No system decoder for this image on API ${Build.VERSION.SDK_INT}")
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: error("Cannot decode image")
        bitmap = decoded
    }

    override fun frameCount(): Int = if (bitmap == null) 0 else 1
    override fun videoWidth(): Int = bitmap?.width ?: 0
    override fun videoHeight(): Int = bitmap?.height ?: 0
    override fun durationUs(): Long = STILL_DURATION_US
    override fun ptsUs(index: Int): Long = 0L
    override fun decodeFrame(index: Int): Pair<Long, Bitmap>? = bitmap?.copy(Bitmap.Config.ARGB_8888, false)?.let { 0L to it }

    override fun close() {
        bitmap?.recycle()
        bitmap = null
    }

    private companion object {
        const val FIRST_AVIF_API = 31
        const val STILL_DURATION_US = 1_000_000L
    }
}
