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

    @Synchronized
    fun sync(context: Context, prefs: AppPrefs) {
        val pm = context.packageManager
        val target = targetKey(context, prefs)
        if (!isEnabled(context, pm, target)) setEnabled(context, pm, target, true)
        allKeys.forEach { if (it != target && isEnabled(context, pm, it)) setEnabled(context, pm, it, false) }
    }

    @Synchronized
    fun healDuplicates(context: Context, prefs: AppPrefs) {
        val pm = context.packageManager
        val enabled = allKeys.filter { isEnabled(context, pm, it) }
        if (enabled.size == 1) return
        if (enabled.isEmpty()) { setEnabled(context, pm, targetKey(context, prefs), true); return }
        val keep = targetKey(context, prefs).takeIf { it in enabled } ?: enabled.first()
        enabled.forEach { if (it != keep) setEnabled(context, pm, it, false) }
    }

    private fun componentFor(context: Context, key: String): ComponentName {
        val cls = key.split('_').joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }
        return ComponentName(context.packageName, "com.frameout.app.icon.$cls")
    }

    private fun isEnabled(context: Context, pm: PackageManager, key: String): Boolean = runCatching {
        when (pm.getComponentEnabledSetting(componentFor(context, key))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> key == DEFAULT_KEY // manifest default: only the indigo/dark alias is enabled
        }
    }.getOrDefault(false)

    private fun setEnabled(context: Context, pm: PackageManager, key: String, enabled: Boolean) {
        runCatching {
            val flag = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(componentFor(context, key), flag, PackageManager.DONT_KILL_APP)
        }
    }

    private fun isSystemDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
