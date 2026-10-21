package com.frameout.engine.work

import android.content.Context
import java.util.UUID

class FgsBudget private constructor(private val projectId: Long, private val startedAt: Long) {
    fun release(context: Context) {
        Companion.release(context, projectId, startedAt)
    }

    companion object {
        private const val PREFS = "frameout_fgs_budget"
        private const val USED = "used_ms"
        private const val ACTIVE = "active"
        private const val WINDOW_START = "window_start"
        private const val ROTATED = "rotated"
        private const val WINDOW_MS = 24L * 60L * 60L * 1000L
        private const val MIN_RETRY_MS = 15L * 60L * 1000L
        private const val LIMIT = 5L * 60L * 60L * 1000L + 30L * 60L * 1000L
        private const val ABANDONED_GRACE_MS = 5L * 60L * 1000L
        private val lock = Any()
        private val processToken = UUID.randomUUID().toString()
        private val sessions = mutableMapOf<Long, Long>()
        private var loaded = false

        fun tryAcquire(context: Context, projectId: Long): FgsBudget? = synchronized(lock) {
            val now = System.currentTimeMillis()
            ensureLoaded(context, now)
            accrue(context, now)
            sessions[projectId]?.let { started -> return FgsBudget(projectId, started) }
            if (used(context) >= LIMIT) return null
            sessions[projectId] = now
            persistSessions(context)
            FgsBudget(projectId, now)
        }

        fun shouldRotate(context: Context, projectId: Long): Boolean = synchronized(lock) {
            val now = System.currentTimeMillis()
            ensureLoaded(context, now)
            accrue(context, now)
            val started = sessions[projectId] ?: return true
            used(context) >= LIMIT || duration(now, started) >= LIMIT
        }

        fun reset(context: Context) = synchronized(lock) {
            val now = System.currentTimeMillis()
            ensureLoaded(context, now)
            sessions.keys.toList().forEach { sessions[it] = now }
            prefs(context).edit().putLong(USED, 0L).putLong(WINDOW_START, now).apply()
            persistSessions(context)
        }

        fun retryDelayMs(context: Context): Long = synchronized(lock) {
            val now = System.currentTimeMillis()
            ensureLoaded(context, now)
            accrue(context, now)
            val start = prefs(context).getLong(WINDOW_START, now)
            (start + WINDOW_MS - now).coerceAtLeast(MIN_RETRY_MS)
        }

        fun markRotated(context: Context, projectId: Long) = synchronized(lock) {
            val ids = rotated(context) + projectId
            prefs(context).edit().putStringSet(ROTATED, ids.map { it.toString() }.toSet()).apply()
        }

        fun clearRotated(context: Context, projectId: Long) = synchronized(lock) {
            val ids = rotated(context) - projectId
            prefs(context).edit().putStringSet(ROTATED, ids.map { it.toString() }.toSet()).apply()
        }

        fun takeRotated(context: Context, projectId: Long): Boolean = synchronized(lock) {
            val ids = rotated(context)
            if (projectId !in ids) return false
            prefs(context).edit().putStringSet(ROTATED, (ids - projectId).map { it.toString() }.toSet()).apply()
            true
        }

        private fun rotated(context: Context): Set<Long> =
            prefs(context).getStringSet(ROTATED, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

        private fun release(context: Context, projectId: Long, fallbackStart: Long) = synchronized(lock) {
            val now = System.currentTimeMillis()
            ensureLoaded(context, now)
            accrue(context, now)
            val started = sessions.remove(projectId) ?: fallbackStart
            val extra = duration(now, started)
            prefs(context).edit().putLong(USED, (used(context) + extra).coerceAtMost(LIMIT)).apply()
            persistSessions(context)
        }

        private fun ensureLoaded(context: Context, now: Long) {
            if (loaded) return
            loaded = true
            val raw = prefs(context).getString(ACTIVE, null) ?: return
            if (raw.isBlank()) return
            var abandoned = 0L
            raw.split(",").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size < 3) return@forEach
                val id = parts[0].toLongOrNull() ?: return@forEach
                val started = parts[1].toLongOrNull() ?: return@forEach
                val token = parts[2]
                if (token == processToken) sessions[id] = started
                else abandoned += duration(now, started).coerceAtMost(ABANDONED_GRACE_MS)
            }
            if (abandoned > 0L) {
                prefs(context).edit().putLong(USED, (used(context) + abandoned).coerceAtMost(LIMIT)).apply()
            }
            persistSessions(context)
        }

        private fun rollWindow(context: Context, now: Long) {
            val start = prefs(context).getLong(WINDOW_START, 0L)
            if (start <= 0L) {
                prefs(context).edit().putLong(WINDOW_START, now).apply()
                return
            }
            if (now - start >= WINDOW_MS || now < start) {
                prefs(context).edit().putLong(USED, 0L).putLong(WINDOW_START, now).apply()
            }
        }

        private fun accrue(context: Context, now: Long) {
            rollWindow(context, now)
            if (sessions.isEmpty()) return
            val added = sessions.values.sumOf { duration(now, it) }
            if (added <= 0L) return
            prefs(context).edit().putLong(USED, (used(context) + added).coerceAtMost(LIMIT)).apply()
            sessions.keys.toList().forEach { sessions[it] = now }
            persistSessions(context)
        }

        private fun used(context: Context): Long =
            prefs(context).getLong(USED, 0L).coerceIn(0L, LIMIT)

        private fun persistSessions(context: Context) {
            val raw = sessions.entries.joinToString(",") { "${it.key}:${it.value}:$processToken" }
            prefs(context).edit().putString(ACTIVE, raw).apply()
        }

        private fun prefs(context: Context) =
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        private fun duration(now: Long, startedAt: Long): Long {
            val delta = now - startedAt
            return when {
                delta < 0L -> 0L
                delta > LIMIT -> LIMIT
                else -> delta
            }
        }
    }
}
