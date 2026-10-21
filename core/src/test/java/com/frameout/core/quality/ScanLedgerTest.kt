package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanLedgerTest {
    private fun ledger(attempts: MutableMap<Long, Int> = HashMap()) = ScanLedger(attempts, maxAttempts = 2, breakerLimit = 3)

    @Test
    fun partialFrameIsRetriedOnceThenKept() {
        val attempts = HashMap<Long, Int>()
        assertFalse(ledger(attempts).markPartial(1, listOf("labels")))
        assertTrue(ledger(attempts).markPartial(1, listOf("labels")))
    }

    @Test
    fun completeClearsEarlierAttempts() {
        val attempts = hashMapOf(5L to 1)
        ledger(attempts).complete(5)
        assertFalse(attempts.containsKey(5L))
    }

    @Test
    fun consecutiveTotalFailuresTripTheBreaker() {
        val ledger = ledger()
        repeat(2) { ledger.allFailed(it.toLong(), listOf("face")) }
        assertFalse(ledger.tripped)
        ledger.allFailed(9, listOf("face"))
        assertTrue(ledger.tripped)
    }

    @Test
    fun aGoodFrameResetsTheBreaker() {
        val ledger = ledger()
        repeat(2) { ledger.allFailed(it.toLong(), listOf("face")) }
        ledger.complete(7)
        ledger.allFailed(8, listOf("face"))
        assertFalse(ledger.tripped)
    }

    @Test
    fun unreadableFramesNeverTripTheBreaker() {
        val ledger = ledger()
        repeat(10) { ledger.unreadable(it.toLong(), "decode") }
        assertFalse(ledger.tripped)
        assertEquals(10, ledger.failed)
        assertEquals(0, ledger.done)
    }

    @Test
    fun aDetectorThatKeepsFailingIsMarkedUnavailableAndStopsRetrying() {
        val ledger = ledger()
        assertFalse(ledger.markPartial(1, listOf("labels")))
        assertFalse(ledger.markPartial(2, listOf("labels")))
        assertTrue(ledger.markPartial(3, listOf("labels")))
        assertEquals(listOf("labels"), ledger.report(3, false).unavailable)
    }

    @Test
    fun exhaustedFramesAreCountedFailedOnlyOnce() {
        val ledger = ledger(hashMapOf(4L to 2))
        assertTrue(ledger.exhausted(4))
        ledger.skip(4)
        ledger.skip(4)
        assertEquals(1, ledger.failed)
        assertTrue(ledger.isParked(4))
    }

    @Test
    fun verdictSeparatesFailureFromPartialAndComplete() {
        assertEquals(ScanVerdict.Complete, ScanReport(4, 4, 0, 0, emptyMap(), false).verdict)
        assertEquals(ScanVerdict.Partial, ScanReport(4, 2, 1, 1, emptyMap(), false).verdict)
        assertEquals(ScanVerdict.Failed, ScanReport(4, 0, 0, 4, emptyMap(), false).verdict)
        assertEquals(ScanVerdict.Stopped, ScanReport(4, 0, 0, 3, emptyMap(), true).verdict)
        assertEquals(ScanVerdict.Complete, ScanReport.EMPTY.verdict)
    }
}
