package com.frameout.core.util

import com.frameout.core.R

object ClipStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun saveClip() = s(R.string.clip_save)
    fun saveAs() = s(R.string.clip_save_as)
    fun saveWait() = s(R.string.clip_save_wait)
    fun mp4() = s(R.string.clip_mp4)
    fun webp() = s(R.string.clip_webp)
    fun tooShort() = s(R.string.clip_too_short)
    fun webpLimit(seconds: Int) = s(R.string.clip_webp_limit, seconds)
    fun volumeNav() = s(R.string.volume_nav)
}
