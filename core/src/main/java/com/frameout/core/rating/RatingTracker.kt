package com.frameout.core.rating

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RatingTracker(
    private val prefs: SharedPreferences,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var failedThisSession = false

    init {
        if (prefs.getLong(KEY_FIRST, 0L) <= 0L) prefs.edit().putLong(KEY_FIRST, clock()).apply()
    }

    fun state(): RatingState = RatingState(
        firstSeenAt = prefs.getLong(KEY_FIRST, 0L),
        sessions = prefs.getInt(KEY_SESSIONS, 0),
        successesSinceAsk = prefs.getInt(KEY_SUCCESSES, 0),
        lifetimeSuccesses = prefs.getInt(KEY_LIFETIME, 0),
        lastAskAt = prefs.getLong(KEY_LAST_ASK, 0L),
        asks = prefs.getInt(KEY_ASKS, 0),
        finished = prefs.getBoolean(KEY_FINISHED, false)
    )

    fun noteSession() {
        failedThisSession = false
        prefs.edit().putInt(KEY_SESSIONS, prefs.getInt(KEY_SESSIONS, 0) + 1).apply()
    }

    fun noteSuccess() {
        prefs.edit()
            .putInt(KEY_SUCCESSES, prefs.getInt(KEY_SUCCESSES, 0) + 1)
            .putInt(KEY_LIFETIME, prefs.getInt(KEY_LIFETIME, 0) + 1)
            .apply()
        refresh()
    }

    fun noteFailure() {
        failedThisSession = true
        _ready.value = false
    }

    fun markShown() {
        prefs.edit()
            .putInt(KEY_ASKS, prefs.getInt(KEY_ASKS, 0) + 1)
            .putLong(KEY_LAST_ASK, clock())
            .putInt(KEY_SUCCESSES, 0)
            .apply()
        _ready.value = false
    }

    fun markFinished() {
        prefs.edit().putBoolean(KEY_FINISHED, true).apply()
        _ready.value = false
    }

    private fun refresh() {
        _ready.value = RatingPolicy.eligible(state(), clock(), failedThisSession)
    }

    companion object {
        private const val PREFS = "frameout_rating"
        private const val KEY_FIRST = "first_seen_at"
        private const val KEY_SESSIONS = "sessions"
        private const val KEY_SUCCESSES = "successes_since_ask"
        private const val KEY_LIFETIME = "lifetime_successes"
        private const val KEY_LAST_ASK = "last_ask_at"
        private const val KEY_ASKS = "asks"
        private const val KEY_FINISHED = "finished"

        @Volatile private var instance: RatingTracker? = null

        fun init(context: Context): RatingTracker =
            instance ?: synchronized(this) {
                instance ?: RatingTracker(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)).also { instance = it }
            }

        fun get(): RatingTracker? = instance
    }
}

object Rating {
    fun success() { RatingTracker.get()?.noteSuccess() }
    fun failure() { RatingTracker.get()?.noteFailure() }
}
