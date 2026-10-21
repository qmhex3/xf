package com.frameout.core.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object StringSource {
    @Volatile private var context: Context? = null
    @Volatile private var cached: Pair<Locale, Resources>? = null

    fun init(app: Context) {
        context = app.applicationContext
    }

    fun get(id: Int, vararg args: Any): String {
        val resources = resourcesFor(Locale.getDefault()) ?: return ""
        return if (args.isEmpty()) resources.getString(id) else resources.getString(id, *args)
    }

    fun plural(id: Int, count: Int, vararg args: Any): String {
        val resources = resourcesFor(Locale.getDefault()) ?: return ""
        return resources.getQuantityString(id, count, *args)
    }

    private fun resourcesFor(locale: Locale): Resources? {
        cached?.takeIf { it.first == locale }?.let { return it.second }
        val base = context ?: return null
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config).resources.also { cached = locale to it }
    }
}
