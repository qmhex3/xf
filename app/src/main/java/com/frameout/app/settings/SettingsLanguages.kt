package com.frameout.app.settings

import com.frameout.core.util.FrameStrings

private class AppLanguage(val key: String, val nativeName: String)

private val AppLanguages = listOf(
    AppLanguage("ar", "العربية"),
    AppLanguage("en", "English")
)

internal fun languageOptions(): List<ChoiceOption> = buildList {
    add(ChoiceOption("auto", FrameStrings.settingValue("auto")))
    AppLanguages.forEach { add(ChoiceOption(it.key, it.nativeName, tag = it.key.uppercase())) }
}

internal fun languageLabel(key: String): String = languageOptions().firstOrNull { it.key == key }?.label ?: key
