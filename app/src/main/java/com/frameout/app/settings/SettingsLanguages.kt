package com.frameout.app.settings

import com.frameout.core.util.FrameStrings

private class AppLanguage(val key: String, val nativeName: String)

private val AppLanguages = listOf(
    AppLanguage("ar", "العربية"),
    AppLanguage("en", "English"),
    AppLanguage("tr", "Türkçe"),
    AppLanguage("ru", "Русский"),
    AppLanguage("fa", "فارسی"),
    AppLanguage("ur", "اردو"),
    AppLanguage("fr", "Français"),
    AppLanguage("pt", "Português"),
    AppLanguage("hi", "हिन्दी"),
    AppLanguage("ko", "한국어"),
    AppLanguage("ja", "日本語"),
    AppLanguage("zh", "简体中文"),
    AppLanguage("zh-TW", "繁體中文"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("ro", "Română"),
    AppLanguage("es", "Español"),
    AppLanguage("uk", "Українська"),
    AppLanguage("it", "Italiano"),
    AppLanguage("nb", "Norsk bokmål"),
    AppLanguage("ms", "Bahasa Melayu"),
    AppLanguage("id", "Bahasa Indonesia"),
    AppLanguage("bn", "বাংলা"),
    AppLanguage("th", "ไทย"),
    AppLanguage("vi", "Tiếng Việt"),
    AppLanguage("nl", "Nederlands"),
    AppLanguage("pl", "Polski"),
    AppLanguage("sv", "Svenska"),
    AppLanguage("fil", "Filipino")
)

internal fun languageOptions(): List<ChoiceOption> = buildList {
    add(ChoiceOption("auto", FrameStrings.settingValue("auto")))
    AppLanguages.forEach { add(ChoiceOption(it.key, it.nativeName, tag = it.key.uppercase())) }
}

internal fun languageLabel(key: String): String = languageOptions().firstOrNull { it.key == key }?.label ?: key
