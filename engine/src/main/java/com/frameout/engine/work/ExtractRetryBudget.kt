package com.frameout.engine.work

import android.content.Context
import com.frameout.codecpack.MediaFailure

object ExtractRetryBudget {
    const val MAX_RETRIES = 2
    private const val PREFS = "frameout_extract_retry"
    private val RETRYABLE = setOf(MediaFailure.Unknown, MediaFailure.IoError, MediaFailure.Internal)

    private fun key(projectId: Long) = "p$projectId"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun used(context: Context, projectId: Long, generation: Long): Int {
        val parts = prefs(context).getString(key(projectId), null)?.split(':') ?: return 0
        val storedGeneration = parts.getOrNull(0)?.toLongOrNull() ?: return 0
        if (storedGeneration != generation) return 0
        return parts.getOrNull(1)?.toIntOrNull() ?: 0
    }

    @Synchronized
    fun tryConsume(context: Context, projectId: Long, generation: Long): Boolean {
        val used = used(context, projectId, generation)
        if (used >= MAX_RETRIES) return false
        prefs(context).edit().putString(key(projectId), "$generation:${used + 1}").apply()
        return true
    }

    @Synchronized
    fun reset(context: Context, projectId: Long) {
        prefs(context).edit().remove(key(projectId)).apply()
    }

    fun isRetryable(reason: MediaFailure?): Boolean = reason == null || reason in RETRYABLE
}
