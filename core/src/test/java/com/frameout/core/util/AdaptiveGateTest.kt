package com.frameout.core.util

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveGateTest {
    private fun peak(gate: AdaptiveGate, jobs: Int): Int = runBlocking {
        val active = AtomicInteger()
        val peak = AtomicInteger()
        coroutineScope {
            repeat(jobs) {
                launch(Dispatchers.Default) {
                    gate.withPermit {
                        peak.accumulateAndGet(active.incrementAndGet()) { a, b -> maxOf(a, b) }
                        delay(15)
                        active.decrementAndGet()
                    }
                }
            }
        }
        peak.get()
    }

    @Test
    fun limitOneSerialisesEveryCaller() {
        assertEquals(1, peak(AdaptiveGate(4) { 1 }, 8))
    }

    @Test
    fun limitIsCappedAtTheHardMaximum() {
        assertTrue(peak(AdaptiveGate(3) { 10 }, 12) <= 3)
    }

    @Test
    fun limitFollowsTheDeviceWhileRunning() {
        var limit = 1
        val gate = AdaptiveGate(4) { limit }
        assertEquals(1, peak(gate, 6))
        limit = 3
        assertTrue(peak(gate, 12) in 2..3)
    }

    @Test
    fun nonPositiveLimitStillRuns() {
        assertEquals(1, peak(AdaptiveGate(2) { 0 }, 4))
    }
}
