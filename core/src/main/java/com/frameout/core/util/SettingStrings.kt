package com.frameout.core.util

import com.frameout.core.R

object SettingStrings {
    private fun s(id: Int): String = StringSource.get(id)
    fun value(key: String): String = when (key) {
        "Dark" -> s(R.string.value_dark)
        "Light" -> s(R.string.value_light)
        "System" -> s(R.string.value_system)
        "AMOLED" -> s(R.string.value_black)
        "Full" -> s(R.string.value_full)
        "Simple" -> s(R.string.value_simple)
        "Off" -> s(R.string.value_off)
        "auto" -> s(R.string.value_auto)
        "jpg" -> "JPG"
        "jpg_hdr" -> "JPG HDR"
        "png" -> "PNG"
        "png_hdr16" -> "PNG HDR 16-bit"
        "webp" -> s(R.string.value_webp)
        "heic" -> "HEIC"
        "avif" -> "AVIF"
        "frame" -> s(R.string.value_frame)
        "video" -> s(R.string.value_video)
        "every_frame" -> s(R.string.value_every_frame)
        "every_n" -> s(R.string.value_every_n)
        "every_time" -> s(R.string.value_every_time)
        "keyframes" -> s(R.string.value_keyframes)
        "fixed" -> s(R.string.value_fixed)
        "scene" -> s(R.string.value_scene)
        "slides" -> s(R.string.value_slides)
        "content" -> s(R.string.value_content)
        "Pictures/FrameOut" -> "Pictures/FrameOut"
        "Pictures" -> "Pictures"
        "DCIM/FrameOut" -> "DCIM/FrameOut"
        "Movies/FrameOut" -> "Movies/FrameOut"
        "80" -> "80"
        "92" -> "92"
        "100" -> "100"
        else -> languageName(key)
    }
    private fun languageName(key: String): String = when (key) {
        "en" -> "English"
        "ar" -> "العربية"
        "tr" -> "Türkçe"
        "ru" -> "Русский"
        "fa" -> "فارسی"
        "ur" -> "اردو"
        "fr" -> "Français"
        "pt" -> "Português"
        "hi" -> "हिन्दी"
        "ko" -> "한국어"
        "ja" -> "日本語"
        "zh" -> "简体中文"
        "zh-TW" -> "繁體中文"
        "de" -> "Deutsch"
        "ro" -> "Română"
        "es" -> "Español"
        "uk" -> "Українська"
        "it" -> "Italiano"
        "nb" -> "Norsk bokmål"
        "ms" -> "Bahasa Melayu"
        "id" -> "Bahasa Indonesia"
        "bn" -> "বাংলা"
        "th" -> "ไทย"
        "vi" -> "Tiếng Việt"
        "nl" -> "Nederlands"
        "pl" -> "Polski"
        else -> size(key)
    }
    private fun size(key: String): String = when (key) {
        "original" -> s(R.string.value_size_original)
        "half" -> s(R.string.value_size_half)
        "max_3840" -> "3840 px"
        "max_1920" -> "1920 px"
        "max_1280" -> "1280 px"
        else -> accentName(key)
    }
    fun accentLabel(key: String): String = accentName(key)
    private fun accentName(key: String): String = when (key) {
        "indigo" -> s(R.string.accent_indigo)
        "ocean" -> s(R.string.accent_ocean)
        "emerald" -> s(R.string.accent_emerald)
        "sunset" -> s(R.string.accent_sunset)
        "rose" -> s(R.string.accent_rose)
        "gold" -> s(R.string.accent_gold)
        "black" -> s(R.string.accent_black)
        "dynamic" -> s(R.string.accent_dynamic)
        else -> key
    }
    fun status(key: String): String = when (key) {
        "Queued" -> FrameStrings.queued()
        "Indexing" -> FrameStrings.indexing()
        "Extracting" -> FrameStrings.extracting()
        "Paused" -> FrameStrings.paused()
        "Completed" -> FrameStrings.completed()
        "Cancelled" -> FrameStrings.cancelled()
        "Failed" -> FrameStrings.failed()
        else -> FrameStrings.working()
    }
}
