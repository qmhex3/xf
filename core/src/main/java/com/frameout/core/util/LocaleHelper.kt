package com.frameout.core.util
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale
object LocaleHelper {
    fun apply(context: Context, language: String): Context {
        val tag = if (language == "auto") {
            AppLang.of(Resources.getSystem().configuration.locales.get(0).toLanguageTag())
        } else {
            language
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return if (Build.VERSION.SDK_INT >= 24) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
}
