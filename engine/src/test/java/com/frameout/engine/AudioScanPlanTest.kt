package com.frameout.engine

import com.frameout.engine.audio.AudioScanPlan
import com.frameout.engine.audio.LevelAccumulator
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioScanPlanTest {
    private val minute = 60_000_000L

    @Test
    fun shortAudioIsScannedWhole() {
        val plan = AudioScanPlan.segments(5 * minute)
        assertEquals(1, plan.size)
        assertEquals(0L, plan[0].startUs)
        assertEquals(5 * minute, plan[0].endUs)
    }

    @Test
    fun audioAtBudgetIsScannedWhole() {
        val plan = AudioScanPlan.segments(AudioScanPlan.BUDGET_US)
        assertEquals(1, plan.size)
        assertEquals(AudioScanPlan.BUDGET_US, plan[0].endUs)
    }

    @Test
    fun unknownDurationIsCappedAtBudget() {
        val plan = AudioScanPlan.segments(0L)
        assertEquals(1, plan.size)
        assertEquals(AudioScanPlan.BUDGET_US, plan[0].endUs)
    }

    @Test
    fun longAudioIsSampledWithinBudgetAndSpansTheWholeFile() {
        val duration = 120 * minute
        val plan = AudioScanPlan.segments(duration)
        assertEquals(AudioScanPlan.BUDGET_US, plan.sumOf { it.endUs - it.startUs })
        assertEquals(0L, plan.first().startUs)
        assertEquals(duration, plan.last().endUs)
        assertTrue(plan.zipWithNext().all { (a, b) -> a.endUs <= b.startUs })
    }

    @Test
    fun startAtPadsSkippedTimeWithSilence() {
        val acc = LevelAccumulator(100)
        acc.configure(12_000, 1, false)
        val buffer = ByteBuffer.allocate(1_200 * 2).order(ByteOrder.nativeOrder())
        repeat(1_200) { buffer.putShort(16_384) }
        acc.feed(buffer, 0, buffer.capacity())
        acc.startAt(1_000_000L)
        acc.feed(buffer, 0, buffer.capacity())
        val levels = acc.finish()
        assertEquals(11, levels.size)
        assertEquals(0.5f, levels[0], 0.01f)
        assertEquals(0f, levels[5], 0.001f)
        assertEquals(0.5f, levels[10], 0.01f)
    }
}
