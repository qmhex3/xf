package com.frameout.core.util

import java.util.Locale

object AppLang {
    val supported: List<String> = listOf("ar", "en", "tr", "ru", "fa", "ur", "fr", "hi", "ko", "ja", "zh")

    fun of(language: String?): String {
        val code = language.orEmpty().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
        return if (code in supported) code else "en"
    }

    fun current(): String = of(Locale.getDefault().language)
}
