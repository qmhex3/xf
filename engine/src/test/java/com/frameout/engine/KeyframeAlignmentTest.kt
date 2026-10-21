package com.frameout.engine

import com.frameout.engine.edit.KeptRange
import com.frameout.engine.edit.SampleDecision
import com.frameout.engine.edit.SampleGate
import com.frameout.engine.edit.alignRangesToKeyframes
import com.frameout.engine.edit.buildKeptRanges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyframeAlignmentTest {
    private val keyframes = listOf(0L, 2_000_000L, 4_000_000L)
    private fun before(us: Long): Long? = keyframes.lastOrNull { it <= us }

    @Test
    fun startMovesBackToPreviousKeyframe() {
        val aligned = alignRangesToKeyframes(listOf(KeptRange(2_500_000L, 3_000_000L)), ::before)!!
        assertEquals(listOf(KeptRange(2_000_000L, 3_000_000L)), aligned)
    }

    @Test
    fun rangesSharingAKeyframeAreMerged() {
        val aligned = alignRangesToKeyframes(listOf(KeptRange(2_100_000L, 2_300_000L), KeptRange(2_600_000L, 3_000_000L)), ::before)!!
        assertEquals(listOf(KeptRange(2_000_000L, 3_000_000L)), aligned)
    }

    @Test
    fun missingKeyframeFailsAlignment() {
        assertNull(alignRangesToKeyframes(listOf(KeptRange(10L, 20L))) { null })
    }

    @Test
    fun buildKeptRangesAlignsWhenAsked() {
        val ranges = buildKeptRanges(listOf(2_500_000L, 2_533_000L), keyframeAtOrBefore = ::before)
        assertEquals(2_000_000L, ranges.single().startUs)
    }

    @Test
    fun firstWrittenVideoSampleMustBeSync() {
        val gate = SampleGate(KeptRange(0L, 1_000_000L), 0L, 0)
        assertEquals(SampleDecision.Skip, gate.decide(0, 0L, false))
        assertEquals(SampleDecision.Write(33_000L), gate.decide(0, 33_000L, true))
    }
}
