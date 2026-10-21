package com.frameout.core.quality

import com.frameout.core.quality.SyntheticFrames.H
import com.frameout.core.quality.SyntheticFrames.W
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionEstimateTest {
    private val base = SyntheticFrames.scene(12)
    private fun profile(l: IntArray) = MotionEstimate.profile(l, W, H)!!

    @Test
    fun stillCameraHasNoSpeed() {
        val speed = MotionEstimate.speed(profile(base), profile(SyntheticFrames.noisy(base, 2.0, 3)))
        assertNotNull(speed)
        assertEquals(0.0, speed!!.toDouble(), 0.002)
    }

    @Test
    fun panIsMeasuredInFractionOfTheFrame() {
        val speed = MotionEstimate.speed(profile(base), profile(SyntheticFrames.panned(base, W, H, 8)))!!
        assertEquals(8.0 / W, speed.toDouble(), 0.01)
    }

    @Test
    fun aCutIsNotMistakenForMotion() {
        assertNull(MotionEstimate.speed(profile(SyntheticFrames.scene(1)), profile(SyntheticFrames.scene(2))))
    }

    @Test
    fun speedsTakeTheBusierNeighbour() {
        val profiles = listOf(base, base, SyntheticFrames.panned(base, W, H, 8), SyntheticFrames.panned(base, W, H, 8)).map { profile(it) }
        val speeds = MotionEstimate.speeds(profiles)
        assertEquals(0f, speeds[0], 0.002f)
        assertTrue(speeds[1] > 0.03f && speeds[2] > 0.03f)
        assertEquals(0f, speeds[3], 0.002f)
    }

    @Test
    fun factorStaysBetweenSeventyPercentAndOne() {
        assertEquals(1f, MotionEstimate.factor(0f), 0f)
        assertEquals(0.7f, MotionEstimate.factor(5f), 0.0001f)
        assertTrue(MotionEstimate.factor(0.01f) in 0.7f..1f)
    }

    @Test
    fun tinyOrFlatInputIsSafe() {
        assertNull(MotionEstimate.profile(IntArray(4), 2, 2))
        val flat = profile(IntArray(W * H) { 100 })
        assertEquals(0f, MotionEstimate.speed(flat, flat)!!, 0f)
    }

    @Test
    fun pickerPrefersTheStillMomentWhenScoresAreClose() {
        val frames = listOf(ScoredFrame(0, 0, 10.0, speed = 0.05f), ScoredFrame(1, 1, 9.0, speed = 0f), ScoredFrame(2, 2, 1.0), ScoredFrame(3, 3, 1.0), ScoredFrame(4, 4, 1.0))
        assertEquals(listOf(1L), SharpestPicker.pick(frames))
    }
}
