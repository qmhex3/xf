package com.frameout.core.ads

class AdGovernor(private val store: AdStore, var rules: AdRules) {
    private var sessionStart = 0L
    private var sessionShown = 0
    private var actions = 0
    private var lastShownElapsed = NEVER

    fun startSession(nowElapsed: Long) {
        sessionStart = nowElapsed
        sessionShown = 0
        actions = 0
        lastShownElapsed = NEVER
        store.put(SESSIONS, store.int(SESSIONS) + 1)
    }

    fun noteAction() {
        actions++
    }

    fun check(placement: AdPlacement, active: Boolean, consent: Boolean, nowElapsed: Long, nowWall: Long): AdVerdict {
        if (!active) return deny(DenyReason.Inactive)
        if (!consent) return deny(DenyReason.NoConsent)
        timing(nowElapsed, nowWall)?.let { return deny(it) }
        return caps(placement, nowWall)?.let { deny(it) } ?: AdVerdict.Allow
    }

    private fun timing(nowElapsed: Long, nowWall: Long): DenyReason? {
        val lastWall = store.long(LAST_SHOWN)
        val lastReward = store.long(LAST_REWARD)
        return when {
            store.int(SESSIONS) <= rules.graceSessions -> DenyReason.Grace
            nowElapsed - sessionStart < rules.warmupMs -> DenyReason.Warmup
            nowWall < lastWall || nowWall < lastReward -> DenyReason.ClockBack
            lastReward > 0 && nowWall - lastReward < rules.afterRewardQuietMs -> DenyReason.AfterReward
            lastShownElapsed != NEVER && nowElapsed - lastShownElapsed < rules.minGapMs -> DenyReason.TooSoon
            lastWall > 0 && nowWall - lastWall < rules.minGapMs -> DenyReason.TooSoon
            else -> null
        }
    }

    private fun caps(placement: AdPlacement, nowWall: Long): DenyReason? {
        val needed = if (placement == AdPlacement.ExportDone) (rules.actionsBetween - 1).coerceAtLeast(1) else rules.actionsBetween
        return when {
            actions < needed -> DenyReason.NotEnoughActions
            sessionShown >= rules.maxPerSession -> DenyReason.SessionCap
            shownToday(nowWall) >= rules.maxPerDay -> DenyReason.DailyCap
            else -> null
        }
    }

    fun recordShown(nowElapsed: Long, nowWall: Long) {
        val day = dayOf(nowWall)
        store.put(DAY, day)
        store.put(DAY_SHOWN, shownToday(nowWall) + 1)
        store.put(LAST_SHOWN, nowWall)
        lastShownElapsed = nowElapsed
        sessionShown++
        actions = 0
    }

    fun canGrantReward(nowWall: Long): Boolean = rewardsToday(nowWall) < rules.rewardsPerDay

    fun recordReward(nowWall: Long) {
        val day = dayOf(nowWall)
        store.put(REWARD_DAY, day)
        store.put(DAY_REWARDS, rewardsToday(nowWall) + 1)
        store.put(LAST_REWARD, nowWall)
    }

    fun sessions(): Int = store.int(SESSIONS)

    private fun dayOf(nowWall: Long): Long = maxOf(nowWall / DAY_MS, store.long(DAY), store.long(REWARD_DAY))

    private fun shownToday(nowWall: Long): Int =
        if (store.long(DAY) == dayOf(nowWall)) store.int(DAY_SHOWN) else 0

    private fun rewardsToday(nowWall: Long): Int =
        if (store.long(REWARD_DAY) == dayOf(nowWall)) store.int(DAY_REWARDS) else 0

    private fun deny(reason: DenyReason): AdVerdict = AdVerdict.Deny(reason)

    private companion object {
        const val NEVER = Long.MIN_VALUE
        const val DAY_MS = 86_400_000L
        const val SESSIONS = "ad_sessions"
        const val DAY = "ad_day"
        const val DAY_SHOWN = "ad_day_shown"
        const val LAST_SHOWN = "ad_last_shown"
        const val LAST_REWARD = "ad_last_reward"
        const val REWARD_DAY = "ad_reward_day"
        const val DAY_REWARDS = "ad_day_rewards"
    }
}
