package com.frameout.core.util

import kotlinx.coroutines.CancellationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorHealthTest {
    @After
    fun clear() = DetectorHealth.reset()

    @Test
    fun everyFailureIsCounted() {
        repeat(5) { DetectorHealth.failed("text", IllegalStateException("boom")) }
        assertEquals(5, DetectorHealth.failures("text"))
        assertEquals("IllegalStateException: boom", DetectorHealth.lastError("text"))
    }

    @Test
    fun cancellationIsIgnored() {
        DetectorHealth.failed("text", CancellationException("stop"))
        assertEquals(0, DetectorHealth.failures("text"))
        assertNull(DetectorHealth.lastError("text"))
    }

    @Test
    fun snapshotListsEachDetector() {
        DetectorHealth.failed("a", IllegalStateException("x"))
        DetectorHealth.failed("b", IllegalStateException("y"))
        DetectorHealth.failed("b", IllegalStateException("y"))
        assertEquals(mapOf("a" to 1, "b" to 2), DetectorHealth.snapshot())
    }

    @Test
    fun repeatedFailuresKeepReachingTheLogAtPowersOfTwo() {
        assertTrue(DetectorHealth.isPowerOfTwo(1))
        assertTrue(DetectorHealth.isPowerOfTwo(64))
        assertFalse(DetectorHealth.isPowerOfTwo(0))
        assertFalse(DetectorHealth.isPowerOfTwo(12))
    }

    @Test
    fun aChangedMessageIsLoggedEvenWhenTheCountIsNotAPowerOfTwo() {
        val lines = java.util.concurrent.CopyOnWriteArrayList<String>()
        DiagLog.reset()
        DiagLog.attach(object : DiagSink {
            override fun write(batch: List<String>) {
                lines += batch
            }

            override fun tail(maxBytes: Int) = ""
        })
        repeat(3) { DetectorHealth.failed("labels", IllegalStateException("first")) }
        DetectorHealth.failed("labels", IllegalStateException("second"))
        DiagLog.flush(5_000)
        DiagLog.reset()
        assertTrue(lines.any { it.contains("second") })
        assertFalse(lines.any { it.contains("#3 ") })
    }
}
