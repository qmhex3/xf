package com.frameout.core.rating

data class RatingState(
    val firstSeenAt: Long = 0L,
    val sessions: Int = 0,
    val successesSinceAsk: Int = 0,
    val lifetimeSuccesses: Int = 0,
    val lastAskAt: Long = 0L,
    val asks: Int = 0,
    val finished: Boolean = false
)

object RatingPolicy {
    const val MIN_SUCCESSES = 3
    const val MAX_ASKS = 3
    const val DAY_MS = 24L * 60L * 60L * 1000L

    fun cooldownDays(asks: Int): Int = when {
        asks <= 0 -> 0
        asks == 1 -> 21
        else -> 60
    }

    fun eligible(state: RatingState, now: Long, failedThisSession: Boolean): Boolean {
        if (state.finished) return false
        if (failedThisSession) return false
        if (state.asks >= MAX_ASKS) return false
        if (state.successesSinceAsk < MIN_SUCCESSES) return false
        if (state.asks > 0 && now - state.lastAskAt < cooldownDays(state.asks) * DAY_MS) return false
        return true
    }

    fun isPositive(stars: Int): Boolean = stars >= 4
}
