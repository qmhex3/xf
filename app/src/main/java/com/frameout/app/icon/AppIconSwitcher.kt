package com.frameout.app.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import com.frameout.core.prefs.AppPrefs

object AppIconSwitcher {
    private val accents = listOf("indigo", "ocean", "emerald", "sunset", "rose", "gold", "black", "dynamic")
    private val modes = listOf("dark", "light", "amoled")
    private const val DEFAULT_KEY = "indigo_dark"

    private val allKeys: List<String> = accents.flatMap { a -> modes.map { m -> "${a}_$m" } }

    fun keyFor(accent: String, themeMode: String, systemDark: Boolean): String {
        val a = when {
            accent == "dynamic" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "dynamic" else "indigo"
            accent in accents -> accent
            else -> "indigo"
        }
        val m = when (themeMode) {
            "Light" -> "light"
            "AMOLED" -> "amoled"
            "System" -> if (systemDark) "dark" else "light"
            else -> "dark"
        }
        return "${a}_$m"
    }

    fun targetKey(context: Context, prefs: AppPrefs): String =
        if (prefs.iconFollowsTheme.value) keyFor(prefs.accent.value, prefs.themeMode.value, isSystemDark(context)) else DEFAULT_KEY

    fun sync(context: Context, prefs: AppPrefs) {
        val target = targetKey(context, prefs)
        val pm = context.packageManager
        setEnabled(context, pm, target, true)
        allKeys.forEach { if (it != target) setEnabled(context, pm, it, false) }
    }

    private fun componentFor(context: Context, key: String): ComponentName {
        val cls = key.split('_').joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }
        return ComponentName(context.packageName, "com.frameout.app.icon.$cls")
    }

    private fun setEnabled(context: Context, pm: PackageManager, key: String, enabled: Boolean) {
        runCatching {
            val component = componentFor(context, key)
            val state = pm.getComponentEnabledSetting(component)
            val effective = when (state) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
                else -> key == DEFAULT_KEY // manifest default: only the indigo/dark alias is enabled
            }
            if (effective == enabled) return
            val flag = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(component, flag, PackageManager.DONT_KILL_APP)
        }
    }

    private fun isSystemDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
