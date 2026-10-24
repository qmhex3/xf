package com.frameout.core.rating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingPolicyTest {
    private val day = RatingPolicy.DAY_MS
    private val now = 100 * day
    private val happy = RatingState(firstSeenAt = now - 5 * day, sessions = 5, successesSinceAsk = 4, lifetimeSuccesses = 4)

    @Test fun happyUserIsEligible() = assertTrue(RatingPolicy.eligible(happy, now, false))

    @Test fun neverBeforeEnoughSuccesses() = assertFalse(RatingPolicy.eligible(happy.copy(successesSinceAsk = 2), now, false))

    @Test fun eligibleOnDayOneAfterThreeSuccesses() = assertTrue(RatingPolicy.eligible(RatingState(firstSeenAt = now, successesSinceAsk = 3, lifetimeSuccesses = 3), now, false))

    @Test fun neverAfterAFailureThisSession() = assertFalse(RatingPolicy.eligible(happy, now, true))

    @Test fun neverAfterFinished() = assertFalse(RatingPolicy.eligible(happy.copy(finished = true), now, false))

    @Test fun cooldownAfterFirstAsk() {
        val asked = happy.copy(asks = 1, lastAskAt = now - 10 * day)
        assertFalse(RatingPolicy.eligible(asked, now, false))
        assertTrue(RatingPolicy.eligible(asked.copy(lastAskAt = now - 22 * day), now, false))
    }

    @Test fun cooldownGrowsAndAsksAreCapped() {
        assertEquals(60, RatingPolicy.cooldownDays(2))
        assertFalse(RatingPolicy.eligible(happy.copy(asks = RatingPolicy.MAX_ASKS, lastAskAt = 0L), now, false))
    }

    @Test fun onlyFourAndFiveGoToTheStore() {
        assertFalse(RatingPolicy.isPositive(3))
        assertTrue(RatingPolicy.isPositive(4))
        assertTrue(RatingPolicy.isPositive(5))
    }
}
