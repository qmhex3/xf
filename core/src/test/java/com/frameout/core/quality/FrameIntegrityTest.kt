package com.frameout.core.quality

import com.frameout.core.quality.FrameIntegrity.Finding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameIntegrityTest {
    private fun steady(count: Int, gapUs: Long) = List(count) { it * gapUs }

    private fun input(pts: List<Long>, expected: Int? = pts.size, duration: Long = pts.size * 33L, capture: Double? = null, rate: Boolean = true, tail: Boolean = true, stride: Int = 1) =
        FrameIntegrity.Input(expected, pts.size, pts, duration, rate, tail, stride, capture)

    @Test
    fun steadyVideoIsHealthy() {
        val report = FrameIntegrity.analyze(input(steady(150, 33_333L), duration = 5000L))
        assertTrue(report.healthy)
        assertEquals(30.0, report.measuredFps!!, 0.01)
    }

    @Test
    fun countMismatchIsReported() {
        val report = FrameIntegrity.analyze(input(steady(150, 33_333L), expected = 167, duration = 5000L))
        assertTrue(Finding.CountMismatch in report.findings)
    }

    @Test
    fun missingTailIsReported() {
        val report = FrameIntegrity.analyze(input(steady(120, 33_333L), expected = 120, duration = 6000L))
        assertTrue(Finding.TailShort in report.findings)
        assertTrue(report.tailMs in 1900L..2100L)
    }

    @Test
    fun variableRateIsReported() {
        val pts = steady(20, 33_000L) + steady(20, 16_000L).map { it + 20 * 33_000L }
        val report = FrameIntegrity.analyze(input(pts, duration = 1500L, tail = false))
        assertTrue(Finding.VariableRate in report.findings)
    }

    @Test
    fun duplicateTimestampsAreReported() {
        val report = FrameIntegrity.analyze(input(listOf(0L, 33_000L, 33_000L, 66_000L, 99_000L), tail = false))
        assertEquals(1, report.duplicateTimestamps)
        assertTrue(Finding.DuplicateTimestamps in report.findings)
    }

    @Test
    fun slowMotionNeedsHigherCaptureRate() {
        val slow = FrameIntegrity.analyze(input(steady(60, 33_333L), duration = 2000L, capture = 240.0))
        assertTrue(Finding.SlowMotion in slow.findings)
        val normal = FrameIntegrity.analyze(input(steady(60, 33_333L), duration = 2000L, capture = 30.0))
        assertTrue(Finding.SlowMotion !in normal.findings)
    }

    @Test
    fun strideScalesMeasuredRate() {
        val report = FrameIntegrity.analyze(input(steady(30, 100_000L), expected = null, tail = false, stride = 3))
        assertEquals(30.0, report.measuredFps!!, 0.01)
    }

    @Test
    fun rateIsSkippedWhenNotChecked() {
        val report = FrameIntegrity.analyze(input(steady(30, 100_000L), expected = null, rate = false, tail = false))
        assertNull(report.measuredFps)
        assertTrue(report.healthy)
    }
}
