package com.frameout.core.prefs

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

abstract class SessionPrefs(protected val prefs: SharedPreferences) {
    protected fun <T : Any> save(key: String, value: T, flow: MutableStateFlow<T>) {
        val editor = prefs.edit()
        when (value) {
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Boolean -> editor.putBoolean(key, value)
            else -> error("Unsupported pref type")
        }
        editor.apply()
        flow.value = value
    }

    fun lastVideoUri(): String = prefs.getString(KEY_LAST_URI, "") ?: ""
    fun lastVideoPosition(): Long = prefs.getLong(KEY_LAST_POS, 0L)
    fun setLastVideo(uri: String, positionMs: Long) {
        prefs.edit().putString(KEY_LAST_URI, uri).putLong(KEY_LAST_POS, positionMs.coerceAtLeast(0L)).apply()
    }

    fun gridIndex(projectId: Long): Int = prefs.getInt("grid_index_$projectId", 0)
    fun gridOffset(projectId: Long): Int = prefs.getInt("grid_offset_$projectId", 0)
    fun setGridPosition(projectId: Long, index: Int, offset: Int) {
        prefs.edit().putInt("grid_index_$projectId", index.coerceAtLeast(0)).putInt("grid_offset_$projectId", offset.coerceAtLeast(0)).apply()
    }
    fun gridColumns(): Int = prefs.getInt(KEY_GRID_COLUMNS, 3)
    fun setGridColumns(value: Int) {
        prefs.edit().putInt(KEY_GRID_COLUMNS, value.coerceIn(2, 8)).apply()
    }
    fun contactColumns(): Int = prefs.getInt(KEY_CONTACT_COLUMNS, 4).coerceIn(2, 8)
    fun setContactColumns(value: Int) { prefs.edit().putInt(KEY_CONTACT_COLUMNS, value.coerceIn(2, 8)).apply() }
    fun contactShowTime(): Boolean = prefs.getBoolean(KEY_CONTACT_TIME, true)
    fun setContactShowTime(value: Boolean) { prefs.edit().putBoolean(KEY_CONTACT_TIME, value).apply() }
    fun contactShowNumber(): Boolean = prefs.getBoolean(KEY_CONTACT_NUMBER, true)
    fun setContactShowNumber(value: Boolean) { prefs.edit().putBoolean(KEY_CONTACT_NUMBER, value).apply() }
    fun contactPng(): Boolean = prefs.getBoolean(KEY_CONTACT_PNG, false)
    fun setContactPng(value: Boolean) { prefs.edit().putBoolean(KEY_CONTACT_PNG, value).apply() }
    fun lastLibrarySession(): Long = prefs.getLong(KEY_LAST_SESSION, 0L)
    fun setLastLibrarySession(ms: Long) {
        prefs.edit().putLong(KEY_LAST_SESSION, ms).apply()
    }
    fun suggestionHandledId(): Long = prefs.getLong(KEY_SUGGESTION_HANDLED, -1L)
    fun setSuggestionHandledId(id: Long) {
        prefs.edit().putLong(KEY_SUGGESTION_HANDLED, id).apply()
    }
    fun taggerVersion(): Int = prefs.getInt(KEY_TAGGER_VERSION, 0)
    fun setTaggerVersion(version: Int) {
        prefs.edit().putInt(KEY_TAGGER_VERSION, version).apply()
    }
    fun onboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING, false)
    fun setOnboardingDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply()
    }

    fun notificationAsked(): Boolean = prefs.getBoolean(KEY_NOTIF_ASKED, false)
    fun setNotificationAsked() {
        prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply()
    }

    fun notificationReminderCount(): Int = prefs.getInt(KEY_NOTIF_REMINDERS, 0)
    fun noteNotificationReminderShown() {
        prefs.edit().putInt(KEY_NOTIF_REMINDERS, notificationReminderCount() + 1).apply()
    }
    fun exhaustNotificationReminders() {
        prefs.edit().putInt(KEY_NOTIF_REMINDERS, MAX_NOTIF_REMINDERS).apply()
    }
    fun shouldRemindNotifications(): Boolean = notificationReminderCount() < MAX_NOTIF_REMINDERS

    fun oemGuideDone(): Boolean = prefs.getBoolean(KEY_OEM, false)
    fun recoveryPending(): Boolean = prefs.getBoolean(KEY_RECOVERY, false)
    fun setRecoveryPending(value: Boolean) { prefs.edit().putBoolean(KEY_RECOVERY, value).apply() }

    fun setOemGuideDone() {
        prefs.edit().putBoolean(KEY_OEM, true).apply()
    }

    private companion object {
        const val KEY_LAST_URI = "last_video_uri"
        const val KEY_LAST_POS = "last_video_position"
        const val MAX_NOTIF_REMINDERS = 2
        const val KEY_NOTIF_REMINDERS = "notification_reminder_count"
        const val KEY_OEM = "oem_guide_done"
        const val KEY_NOTIF_ASKED = "notification_permission_asked"
        const val KEY_RECOVERY = "recovery_pending"
        const val KEY_GRID_COLUMNS = "grid_columns"
        const val KEY_CONTACT_COLUMNS = "contact_columns"
        const val KEY_CONTACT_TIME = "contact_show_time"
        const val KEY_CONTACT_NUMBER = "contact_show_number"
        const val KEY_CONTACT_PNG = "contact_png"
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_LAST_SESSION = "last_library_session"
        const val KEY_SUGGESTION_HANDLED = "suggestion_handled_id"
        const val KEY_TAGGER_VERSION = "tagger_version"
    }
}
