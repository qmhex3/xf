package com.frameout.core.ads

class AdGovernor(private val store: AdStore, var rules: AdRules) {
    private var sessionStart = 0L
    private var sessionShown = 0
    private var lastShownElapsed = NEVER

    fun startSession(nowElapsed: Long) {
        sessionStart = nowElapsed
        sessionShown = 0
        lastShownElapsed = NEVER
        store.put(SESSIONS, store.int(SESSIONS) + 1)
    }

    /** Actions are persisted so that closing the app does not reset the progress towards the next ad. */
    fun noteAction() {
        store.put(ACTIONS, (actions() + 1).coerceAtMost(ACTION_CAP))
    }

    private fun actions(): Int = store.int(ACTIONS)

    /**
     * True when an ad was already due (the governor said Allow) but the user has not watched it to the end yet,
     * for example because the ad was not loaded, the screen changed, or the app was closed while it was on screen.
     * The debt survives process death and is only cleared by [settle].
     */
    fun owes(nowWall: Long): Boolean {
        if (store.int(DEBT) <= 0) return false
        val since = nowWall - store.long(DEBT_AT)
        return since <= DEBT_TTL_MS
    }

    /** The owed ad was watched until it was dismissed. */
    fun settle() {
        store.put(DEBT, 0)
        store.put(DEBT_AT, 0L)
    }

    fun check(placement: AdPlacement, active: Boolean, consent: Boolean, nowElapsed: Long, nowWall: Long): AdVerdict {
        if (!active) return deny(DenyReason.Inactive)
        if (!consent) return deny(DenyReason.NoConsent)
        val owed = owes(nowWall)
        timing(nowElapsed, nowWall, owed)?.let { return deny(it) }
        caps(placement, nowWall, owed)?.let { return deny(it) }
        if (!owed) incur(nowWall)
        return AdVerdict.Allow
    }

    private fun incur(nowWall: Long) {
        store.put(DEBT, 1)
        store.put(DEBT_AT, nowWall)
    }

    private fun timing(nowElapsed: Long, nowWall: Long, owed: Boolean): DenyReason? {
        val lastWall = store.long(LAST_SHOWN)
        val lastReward = store.long(LAST_REWARD)
        val warmup = if (owed) minOf(rules.warmupMs, DEBT_WARMUP_MS) else rules.warmupMs
        return when {
            store.int(SESSIONS) <= rules.graceSessions -> DenyReason.Grace
            nowElapsed - sessionStart < warmup -> DenyReason.Warmup
            nowWall < lastWall || nowWall < lastReward -> DenyReason.ClockBack
            lastReward > 0 && nowWall - lastReward < rules.afterRewardQuietMs -> DenyReason.AfterReward
            lastShownElapsed != NEVER && nowElapsed - lastShownElapsed < rules.minGapMs -> DenyReason.TooSoon
            lastWall > 0 && nowWall - lastWall < rules.minGapMs -> DenyReason.TooSoon
            else -> null
        }
    }

    private fun caps(placement: AdPlacement, nowWall: Long, owed: Boolean): DenyReason? {
        val needed = if (placement == AdPlacement.ExportDone) (rules.actionsBetween - 1).coerceAtLeast(1) else rules.actionsBetween
        return when {
            !owed && actions() < needed -> DenyReason.NotEnoughActions
            !owed && sessionShown >= rules.maxPerSession -> DenyReason.SessionCap
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
        store.put(ACTIONS, 0)
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
        const val ACTION_CAP = 50
        const val DEBT_WARMUP_MS = 20_000L
        const val DEBT_TTL_MS = 14 * 86_400_000L
        const val DAY_MS = 86_400_000L
        const val SESSIONS = "ad_sessions"
        const val ACTIONS = "ad_actions"
        const val DEBT = "ad_debt"
        const val DEBT_AT = "ad_debt_at"
        const val DAY = "ad_day"
        const val DAY_SHOWN = "ad_day_shown"
        const val LAST_SHOWN = "ad_last_shown"
        const val LAST_REWARD = "ad_last_reward"
        const val REWARD_DAY = "ad_reward_day"
        const val DAY_REWARDS = "ad_day_rewards"
    }
}
