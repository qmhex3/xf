package com.frameout.core.util

import java.util.Locale

object AppLang {
    val supported: List<String> = listOf("ar", "en", "tr", "ru", "fa", "ur", "fr", "pt", "hi", "ko", "ja", "zh", "zh-TW", "de", "ro", "es", "uk", "it", "nb", "ms", "id", "bn", "th", "vi", "nl", "pl", "sv", "fil")

    private val traditionalRegions = setOf("tw", "hk", "mo")

    fun of(language: String?): String {
        val parts = language.orEmpty().lowercase(Locale.ROOT).replace('_', '-').split('-')
        val code = parts.firstOrNull().orEmpty()
        if (code == "zh") {
            val traditional = "hant" in parts || (("hans" !in parts) && parts.any { it in traditionalRegions })
            return if (traditional) "zh-TW" else "zh"
        }
        val normalized = when (code) {
            "no", "nn" -> "nb"
            "in" -> "id"
            "tl" -> "fil"
            else -> code
        }
        return if (normalized in supported) normalized else "en"
    }

    fun current(): String = of(Locale.getDefault().toLanguageTag())
}
