package com.frameout.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdHealthTest {
    private val now = 1_800_000_000_000L
    private val week = 7 * 86_400_000L

    @Test
    fun noFillIsNotBlocked() {
        val h = AdHealth(FakeAdStore())
        repeat(10) { h.onFailed(AdHealth.NO_FILL, true, false, now) }
        assertEquals(AdState.NoFill, h.state)
    }

    @Test
    fun offlineFailuresAreIgnored() {
        val h = AdHealth(FakeAdStore())
        repeat(10) { h.onFailed(AdHealth.NETWORK_ERROR, false, false, now) }
        assertEquals(AdState.Unknown, h.state)
    }

    @Test
    fun repeatedNetworkErrorsWhileOnlineMeanBlocked() {
        val h = AdHealth(FakeAdStore())
        repeat(3) { h.onFailed(AdHealth.NETWORK_ERROR, true, false, now) }
        assertEquals(AdState.Unknown, h.state)
        h.onFailed(AdHealth.NETWORK_ERROR, true, false, now)
        assertEquals(AdState.Blocked, h.state)
    }

    @Test
    fun dnsSinkIsImmediateBlock() {
        val h = AdHealth(FakeAdStore())
        h.onFailed(AdHealth.NETWORK_ERROR, true, true, now)
        assertEquals(AdState.Blocked, h.state)
    }

    @Test
    fun recentSuccessProtectsFromFalsePositive() {
        val h = AdHealth(FakeAdStore())
        h.onLoaded(now - 1000)
        repeat(6) { h.onFailed(AdHealth.NETWORK_ERROR, true, false, now) }
        assertEquals(AdState.Healthy, h.state)
    }

    @Test
    fun loadedResetsBlock() {
        val h = AdHealth(FakeAdStore())
        h.onFailed(AdHealth.NETWORK_ERROR, true, true, now)
        h.onLoaded(now)
        assertEquals(AdState.Healthy, h.state)
    }

    @Test
    fun noticeShownAtMostWeekly() {
        val h = AdHealth(FakeAdStore())
        h.onFailed(AdHealth.NETWORK_ERROR, true, true, now)
        assertTrue(h.shouldNotify(now))
        h.noticeShown(now)
        assertFalse(h.shouldNotify(now + 1000))
        assertTrue(h.shouldNotify(now + week + 1))
    }
}
