package com.frameout.engine.work

import android.content.Context
import android.content.res.Configuration
import com.frameout.core.prefs.AppPrefs
import com.frameout.designsystem.theme.NotificationAccent
import com.frameout.designsystem.theme.notificationAccent

internal object NotificationTheme {
    private data class Key(val accent: String, val dark: Boolean)

    @Volatile private var cached: Pair<Key, NotificationAccent>? = null

    fun accent(context: Context): NotificationAccent {
        val app = context.applicationContext
        val key = Key(AppPrefs.peekAccent(app), isDark(app, AppPrefs.peekThemeMode(app)))
        cached?.let { if (it.first == key) return it.second }
        val resolved = notificationAccent(key.accent, key.dark, app)
        cached = key to resolved
        return resolved
    }

    fun color(context: Context): Int = accent(context).strong

    private fun isDark(context: Context, mode: String): Boolean = when (mode) {
        "Light" -> false
        "System" -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        else -> true
    }
}
