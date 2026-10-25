package com.frameout.core.util

import java.util.Locale

object AppLang {
    val supported: List<String> = listOf("ar", "en", "tr", "ru", "fa", "ur", "fr", "pt", "hi", "ko", "ja", "zh", "de", "ro", "es", "uk", "it", "nb", "ms")

    fun of(language: String?): String {
        val code = language.orEmpty().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
        val normalized = if (code == "no" || code == "nn") "nb" else code
        return if (normalized in supported) normalized else "en"
    }

    fun current(): String = of(Locale.getDefault().language)
}
