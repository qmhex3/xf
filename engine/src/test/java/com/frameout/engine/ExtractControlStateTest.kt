package com.frameout.engine

import com.frameout.engine.work.ControlResolver
import com.frameout.engine.work.ControlState
import com.frameout.engine.work.ExtractControl
import com.frameout.engine.work.RemainingEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractControlStateTest {
    @Test
    fun emptyMemoryFollowsStoredPause() {
        assertEquals(ControlState.Paused, ControlResolver.resolve(false, false, "Paused"))
    }

    @Test
    fun emptyMemoryFollowsStoredCancel() {
        assertEquals(ControlState.Cancelled, ControlResolver.resolve(false, false, "Cancelled"))
    }

    @Test
    fun cancelBeatsPauseFromEitherSource() {
        assertEquals(ControlState.Cancelled, ControlResolver.resolve(true, false, "Cancelled"))
        assertEquals(ControlState.Cancelled, ControlResolver.resolve(false, true, "Paused"))
    }

    @Test
    fun runningWhenNeitherSourceStops() {
        assertEquals(ControlState.Running, ControlResolver.resolve(false, false, "Extracting"))
        assertEquals(ControlState.Running, ControlResolver.resolve(false, false, null))
    }

    @Test
    fun memoryPauseAloneStillPauses() {
        assertEquals(ControlState.Paused, ControlResolver.resolve(true, false, "Extracting"))
    }

    @Test
    fun storedPauseOrCancelCountsAsUserStop() {
        assertTrue(ControlResolver.stoppedByUser("Paused"))
        assertTrue(ControlResolver.stoppedByUser("Cancelled"))
        assertFalse(ControlResolver.stoppedByUser("Extracting"))
        assertFalse(ControlResolver.stoppedByUser(null))
    }

    @Test
    fun controlSetsBehaveIndependentlyPerProject() {
        ExtractControl.pause(901L)
        assertTrue(ExtractControl.isPaused(901L))
        assertFalse(ExtractControl.isPaused(902L))
        ExtractControl.cancel(901L)
        assertFalse(ExtractControl.isPaused(901L))
        assertTrue(ExtractControl.isCancelled(901L))
        ExtractControl.clear(901L)
        assertFalse(ExtractControl.isCancelled(901L))
    }

    @Test
    fun estimateIgnoresIndexingTime() {
        val estimator = RemainingEstimator()
        assertEquals(0L, estimator.update(false, false, 0, 1000, 0L))
        assertEquals(0L, estimator.update(false, false, 0, 1000, 600_000L))
        assertEquals(0L, estimator.update(true, false, 1, 1000, 600_000L))
        assertEquals(989_000L, estimator.update(true, false, 11, 1000, 610_000L))
    }

    @Test
    fun estimateUsesOnlyFramesDoneInThisRun() {
        val estimator = RemainingEstimator()
        estimator.update(true, false, 500, 1000, 0L)
        assertEquals(80_000L, estimator.update(true, false, 600, 1000, 20_000L))
    }

    @Test
    fun pauseRestartsTheMeasurement() {
        val estimator = RemainingEstimator()
        estimator.update(true, false, 10, 100, 0L)
        assertEquals(0L, estimator.update(true, true, 20, 100, 100_000L))
        assertEquals(0L, estimator.update(true, false, 20, 100, 100_000L))
        assertEquals(7_000L, estimator.update(true, false, 30, 100, 101_000L))
    }

    @Test
    fun estimateNeverGoesNegative() {
        val estimator = RemainingEstimator()
        estimator.update(true, false, 1, 10, 0L)
        assertEquals(0L, estimator.update(true, false, 12, 10, 1_000L))
    }

    @Test
    fun estimateWaitsForWarmupFrames() {
        val estimator = RemainingEstimator()
        estimator.update(true, false, 0, 1000, 0L)
        assertEquals(0L, estimator.update(true, false, 1, 1000, 8_000L))
        assertEquals(0L, estimator.update(true, false, 4, 1000, 9_000L))
        assertEquals(1_990_000L, estimator.update(true, false, 5, 1000, 10_000L))
    }

    @Test
    fun staleGenerationIsNotCurrentAfterNewerRun() {
        val id = 911L
        val old = ExtractControl.generation(id)
        val next = ExtractControl.nextGeneration(id)
        assertTrue(next > old)
        assertFalse(ExtractControl.isCurrent(id, old))
        assertTrue(ExtractControl.isCurrent(id, next))
        ExtractControl.adoptGeneration(id, old)
        assertTrue(ExtractControl.isCurrent(id, next))
        ExtractControl.adoptGeneration(id, next + 5)
        assertTrue(ExtractControl.isCurrent(id, next + 5))
    }

    @Test
    fun onlyOneRunOwnsAProjectAtATime() {
        val id = 912L
        val first = Any()
        val second = Any()
        assertTrue(ExtractControl.tryBegin(id, first))
        assertFalse(ExtractControl.tryBegin(id, second))
        ExtractControl.end(id, second)
        assertTrue(ExtractControl.isActive(id))
        ExtractControl.end(id, first)
        assertFalse(ExtractControl.isActive(id))
        assertTrue(ExtractControl.tryBegin(id, second))
        ExtractControl.end(id, second)
    }

    @Test
    fun awaitIdleReturnsOnceRunEnds() {
        val id = 913L
        val owner = Any()
        assertTrue(ExtractControl.tryBegin(id, owner))
        assertFalse(kotlinx.coroutines.runBlocking { ExtractControl.awaitIdle(id, 120L) })
        ExtractControl.end(id, owner)
        assertTrue(kotlinx.coroutines.runBlocking { ExtractControl.awaitIdle(id, 120L) })
    }
}
