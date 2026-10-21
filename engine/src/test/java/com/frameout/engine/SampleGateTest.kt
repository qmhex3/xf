package com.frameout.engine

import com.frameout.engine.edit.KeptRange
import com.frameout.engine.edit.SampleDecision
import com.frameout.engine.edit.SampleGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleGateTest {
    private fun gate(start: Long = 0L, end: Long = 1_000_000L, base: Long = 0L) =
        SampleGate(KeptRange(start, end), base, 0)

    @Test
    fun decodeOrderWithBFramesKeepsEverySample() {
        val gate = gate()
        val decodeOrderPts = listOf(0L, 133_000L, 67_000L, 33_000L, 100_000L, 267_000L, 200_000L)
        val written = decodeOrderPts.map { gate.decide(0, it, it == 0L) }
        assertTrue(written.all { it is SampleDecision.Write })
        assertEquals(decodeOrderPts, written.map { (it as SampleDecision.Write).outPtsUs })
    }

    @Test
    fun duplicateVideoPtsIsSkipped() {
        val gate = gate()
        gate.decide(0, 100_000L, true)
        assertEquals(SampleDecision.Skip, gate.decide(0, 100_000L, false))
    }

    @Test
    fun leadingFramesBeforeStartAreSkipped() {
        val gate = gate(start = 100_000L, base = 500L)
        assertEquals(SampleDecision.Skip, gate.decide(0, 67_000L, false))
        assertEquals(SampleDecision.Write(500L), gate.decide(0, 100_000L, true))
    }

    @Test
    fun bFrameAfterOutOfRangeReferenceFrameIsStillWritten() {
        val gate = gate(end = 200_000L)
        gate.decide(0, 0L, true)
        assertEquals(SampleDecision.Skip, gate.decide(0, 233_000L, false))
        assertEquals(SampleDecision.Write(167_000L), gate.decide(0, 167_000L, false))
    }

    @Test
    fun syncFramePastEndStopsTheRange() {
        val gate = gate(end = 200_000L)
        assertEquals(SampleDecision.Stop, gate.decide(0, 300_000L, true))
    }

    @Test
    fun longRunPastEndStops() {
        val gate = gate(end = 200_000L)
        var last: SampleDecision = SampleDecision.Skip
        repeat(SampleGate.MAX_REORDER_LOOKAHEAD + 1) { last = gate.decide(0, 300_000L + it, false) }
        assertEquals(SampleDecision.Stop, last)
    }

    @Test
    fun audioMustStayMonotonicAndInRange() {
        val gate = gate(end = 200_000L)
        assertEquals(SampleDecision.Write(10_000L), gate.decide(1, 10_000L, true))
        assertEquals(SampleDecision.Skip, gate.decide(1, 10_000L, true))
        assertEquals(SampleDecision.Skip, gate.decide(1, 250_000L, true))
        assertEquals(SampleDecision.Write(20_000L), gate.decide(1, 20_000L, true))
    }

    @Test
    fun maxOutTracksLargestWrittenPts() {
        val gate = gate()
        gate.decide(0, 0L, true)
        gate.decide(0, 133_000L, false)
        gate.decide(0, 67_000L, false)
        assertEquals(133_000L, gate.maxOutUs)
    }
}
