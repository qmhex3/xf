package com.frameout.core.util

import android.graphics.Bitmap
import com.frameout.core.edit.BitmapEditRenderer
import com.frameout.core.edit.FrameEdit

object EditedThumb {
    private fun FrameEdit?.effective(): FrameEdit? = this?.takeUnless { it.isIdentity }

    fun key(path: String?, edit: FrameEdit?): String? =
        if (path == null) null else edit.effective()?.let { "$path#256@${it.hashCode()}" }

    fun peek(path: String?, edit: FrameEdit?): Bitmap? {
        val key = key(path, edit) ?: return ThumbCache.peek(path)
        return ThumbCache.peekKey(key)
    }

    suspend fun load(path: String?, edit: FrameEdit?): Bitmap? {
        if (path == null) return null
        val effective = edit.effective() ?: return ThumbCache.load(path)
        return ThumbCache.loadKeyed("$path#256@${effective.hashCode()}") {
            ThumbCache.get(path)?.let { source -> BitmapEditRenderer.render(source, effective) }
        }
    }

    fun peekSharp(path: String?, edit: FrameEdit?, px: Int): Bitmap? {
        if (path == null) return null
        val effective = edit.effective() ?: return ThumbCache.peekSharp(path, px)
        return ThumbCache.peekKey("$path#hq$px@${effective.hashCode()}")
    }

    suspend fun loadSharp(path: String?, edit: FrameEdit?, px: Int): Bitmap? {
        if (path == null) return null
        val effective = edit.effective() ?: return ThumbCache.loadSharp(path, px)
        return ThumbCache.loadKeyed("$path#hq$px@${effective.hashCode()}") {
            ThumbCache.decodeSharp(path, px)?.let { source -> BitmapEditRenderer.render(source, effective) }
        }
    }
}
