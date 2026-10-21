package com.frameout.core.util

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

object IntentToken {
    private const val PREFS = "frameout_guard"
    private const val KEY = "intent_token"
    private const val BYTES = 16

    fun get(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { return it }
        val fresh = ByteArray(BYTES).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(KEY, fresh).apply()
        return fresh
    }

    fun matches(context: Context, candidate: String?): Boolean =
        candidate != null && MessageDigest.isEqual(get(context).toByteArray(), candidate.toByteArray())
}
