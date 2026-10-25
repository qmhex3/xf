package com.frameout.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdGovernorTest {
    private val minute = 60_000L
    private val day = 24 * 60 * minute
    private val rules = AdRules.forTier(CountryTier.High)
    private val t0 = 1_800_000_000_000L

    private fun governor(store: AdStore = FakeAdStore(), sessions: Int = 3): AdGovernor {
        val g = AdGovernor(store, rules)
        repeat(sessions) { g.startSession(0L) }
        return g
    }

    private fun ready(g: AdGovernor) = repeat(3) { g.noteAction() }

    private fun check(g: AdGovernor, elapsed: Long, wall: Long = t0 + elapsed, placement: AdPlacement = AdPlacement.ScreenBreak, active: Boolean = true, consent: Boolean = true) =
        g.check(placement, active, consent, elapsed, wall)

    private fun deny(reason: DenyReason) = AdVerdict.Deny(reason)

    @Test
    fun allowsAfterWarmupAndActions() {
        val g = governor()
        ready(g)
        assertEquals(AdVerdict.Allow, check(g, 3 * minute))
    }

    @Test
    fun inactiveOrNoConsentNeverAllows() {
        val g = governor()
        ready(g)
        assertEquals(deny(DenyReason.Inactive), check(g, 3 * minute, active = false))
        assertEquals(deny(DenyReason.NoConsent), check(g, 3 * minute, consent = false))
    }

    @Test
    fun firstSessionsAreAdFree() {
        val g = governor(sessions = 2)
        ready(g)
        assertEquals(deny(DenyReason.Grace), check(g, 10 * minute))
    }

    @Test
    fun warmupBlocksEarlyAd() {
        val g = governor()
        ready(g)
        assertEquals(deny(DenyReason.Warmup), check(g, minute))
    }

    @Test
    fun needsEnoughActions() {
        val g = governor()
        g.noteAction()
        assertEquals(deny(DenyReason.NotEnoughActions), check(g, 3 * minute))
        g.noteAction()
        assertEquals(AdVerdict.Allow, check(g, 3 * minute, placement = AdPlacement.ExportDone))
    }

    @Test
    fun gapEnforcedAfterShown() {
        val g = governor()
        ready(g)
        g.recordShown(3 * minute, t0 + 3 * minute)
        ready(g)
        assertEquals(deny(DenyReason.TooSoon), check(g, 4 * minute))
        assertEquals(AdVerdict.Allow, check(g, 7 * minute))
    }

    @Test
    fun gapSurvivesProcessRestart() {
        val store = FakeAdStore()
        val first = governor(store, 3)
        ready(first)
        first.recordShown(3 * minute, t0 + 3 * minute)
        val second = AdGovernor(store, rules)
        second.startSession(0L)
        ready(second)
        assertEquals(deny(DenyReason.TooSoon), second.check(AdPlacement.ScreenBreak, true, true, 3 * minute, t0 + 4 * minute))
    }

    @Test
    fun sessionCapStopsAds() {
        val g = governor()
        repeat(rules.maxPerSession) { i ->
            ready(g)
            g.recordShown(3 * minute + i * 10 * minute, t0 + 3 * minute + i * 10 * minute)
        }
        ready(g)
        assertEquals(deny(DenyReason.SessionCap), check(g, 60 * minute))
    }

    @Test
    fun dailyCapResetsNextDay() {
        val store = FakeAdStore()
        val g = AdGovernor(store, rules.copy(maxPerSession = 100, maxPerDay = 2))
        repeat(3) { g.startSession(0L) }
        repeat(2) { i ->
            ready(g)
            g.recordShown(3 * minute + i * 10 * minute, t0 + 3 * minute + i * 10 * minute)
        }
        ready(g)
        assertEquals(deny(DenyReason.DailyCap), check(g, 40 * minute))
        val next = AdGovernor(store, rules.copy(maxPerSession = 100, maxPerDay = 2))
        next.startSession(0L)
        ready(next)
        assertEquals(AdVerdict.Allow, next.check(AdPlacement.ScreenBreak, true, true, 3 * minute, t0 + day + 3 * minute))
    }

    @Test
    fun clockSetBackIsDenied() {
        val g = governor()
        ready(g)
        g.recordShown(3 * minute, t0 + 3 * minute)
        ready(g)
        assertEquals(deny(DenyReason.ClockBack), check(g, 20 * minute, wall = t0 - day))
    }

    @Test
    fun rewardSilencesInterstitialsThenCapsRewards() {
        val g = governor()
        ready(g)
        g.recordReward(t0 + 3 * minute)
        assertEquals(deny(DenyReason.AfterReward), check(g, 5 * minute))
        assertEquals(AdVerdict.Allow, check(g, 15 * minute))
        repeat(rules.rewardsPerDay - 1) { g.recordReward(t0 + 3 * minute) }
        assertTrue(!g.canGrantReward(t0 + 3 * minute))
    }

    @Test
    fun lowTierIsGentlerThanHigh() {
        val low = AdRules.forTier(CountryTier.Low)
        assertTrue(low.maxPerDay < rules.maxPerDay && low.minGapMs > rules.minGapMs)
    }
}
