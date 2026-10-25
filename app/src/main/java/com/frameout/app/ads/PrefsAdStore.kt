package com.frameout.app.ads

import android.content.Context
import com.frameout.core.ads.AdStore

class PrefsAdStore(context: Context) : AdStore {
    private val prefs = context.applicationContext.getSharedPreferences("frameout_ads", Context.MODE_PRIVATE)

    override fun long(key: String, default: Long): Long = prefs.getLong(key, default)
    override fun int(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun put(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    override fun put(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
}
