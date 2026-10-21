package com.frameout.app.frames

import android.content.Context

internal object SharpestCache {
    private const val FILE = "sharpest_cache"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isValid(context: Context, projectId: Long, totalFrames: Int): Boolean {
        val raw = prefs(context).getString(projectId.toString(), null) ?: return false
        return raw.substringBefore(':', "").toIntOrNull() == totalFrames
    }

    fun mark(context: Context, projectId: Long, totalFrames: Int) {
        prefs(context).edit().putString(projectId.toString(), "$totalFrames:").apply()
    }

    fun clear(context: Context, projectId: Long) {
        prefs(context).edit().remove(projectId.toString()).apply()
    }
}
