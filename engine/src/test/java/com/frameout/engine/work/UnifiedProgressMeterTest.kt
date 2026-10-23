package com.frameout.engine.work

import com.frameout.engine.extract.ExtractProgress
import com.frameout.engine.extract.ExtractProgress.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedProgressMeterTest {
    private fun p(status: Status, done: Int, total: Int) = ExtractProgress(1L, status, done, total, 0L, 0L)

    @Test
    fun indexingNeverFillsTheBar() {
        val meter = UnifiedProgressMeter()
        val end = meter.update(p(Status.Indexing, 155_082, 155_082))
        assertEquals(UnifiedExtractProgress.INDEX_SHARE, end, 1e-4f)
        assertTrue(end < 1f)
    }

    @Test
    fun overshotIndexEstimateStaysInsideTheIndexShare() {
        val meter = UnifiedProgressMeter()
        val value = meter.update(p(Status.Indexing, 160_000, 155_082))
        assertTrue(value <= UnifiedExtractProgress.INDEX_SHARE + 1e-4f)
    }

    @Test
    fun handoverToExtractionNeverGoesBackwards() {
        val meter = UnifiedProgressMeter()
        val index = meter.update(p(Status.Indexing, 1000, 1000))
        val start = meter.update(p(Status.Extracting, 0, 500))
        val half = meter.update(p(Status.Extracting, 250, 500))
        assertTrue(start >= index)
        assertTrue(half > start)
        assertFalse(meter.isIndexing)
        assertEquals(1f, meter.update(p(Status.Completed, 500, 500)), 1e-4f)
    }

    @Test
    fun pauseKeepsTheLastValue() {
        val meter = UnifiedProgressMeter()
        val before = meter.update(p(Status.Indexing, 500, 1000))
        assertEquals(before, meter.update(p(Status.Paused, 500, 1000)), 1e-4f)
    }
}

class UnifiedStoredProgressTest {
    @Test
    fun indexingWithoutLiveValueIsUnknown() {
        assertEquals(null, UnifiedExtractProgress.stored("Indexing", 0, 155_082, null))
    }

    @Test
    fun extractionFallsBackToTheDatabaseOnTheUnifiedScale() {
        assertEquals(0.6f, UnifiedExtractProgress.stored("Extracting", 500, 1000, null)!!, 1e-4f)
    }

    @Test
    fun liveValueIsUsedButNeverBelowTheStoredOne() {
        assertEquals(0.1f, UnifiedExtractProgress.stored("Indexing", 0, 1000, 0.1f)!!, 1e-4f)
        assertEquals(0.6f, UnifiedExtractProgress.stored("Extracting", 500, 1000, 0.3f)!!, 1e-4f)
    }

    @Test
    fun completedIsFullAndIdleHasNoProgress() {
        assertEquals(1f, UnifiedExtractProgress.stored("Completed", 10, 10, null)!!, 1e-4f)
        assertEquals(null, UnifiedExtractProgress.stored("Failed", 5, 10, 0.5f))
    }
}

class LiveExtractProgressTest {
    @Test
    fun publishKeepsThePeakAndClearForgetsIt() {
        LiveExtractProgress.clear(77L)
        LiveExtractProgress.publish(77L, 0.15f)
        LiveExtractProgress.publish(77L, 0.10f)
        assertEquals(0.15f, LiveExtractProgress.all.value[77L]!!, 1e-4f)
        LiveExtractProgress.clear(77L)
        assertEquals(null, LiveExtractProgress.all.value[77L])
    }
}
