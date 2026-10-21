package com.frameout.app.extract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractCapsuleStateTest {
    @Test
    fun indexingMarksEstimate() {
        val state = ExtractCapsuleState(status = "Indexing", done = 120, total = 980)
        assertTrue(state.estimated)
        assertEquals("", state.counter)
    }

    @Test
    fun indexingBeyondEstimateGrowsTotal() {
        val state = ExtractCapsuleState(status = "Indexing", done = 1200, total = 980)
        assertEquals(1200, state.shownTotal)
    }

    @Test
    fun extractingShowsExactCounter() {
        val state = ExtractCapsuleState(status = "Extracting", done = 412, total = 980)
        assertTrue(state.counter.contains("412/980"))
        assertTrue(!state.counter.contains("≈"))
        assertEquals(42, state.percent)
    }

    @Test
    fun completedIsFull() {
        assertEquals(100, ExtractCapsuleState(status = "Completed", done = 0, total = 0).percent)
    }

    private fun tracked(tracker: UnifiedProgressTracker, status: String, done: Int, total: Int, stored: Int = 0) =
        tracker.update(ExtractCapsuleState(status = status, done = done, total = total), stored)

    @Test
    fun barNeverResetsWhenIndexingHandsOverToExtracting() {
        val tracker = UnifiedProgressTracker()
        val endOfIndexing = tracked(tracker, "Indexing", 980, 980).fraction
        assertEquals(UnifiedProgress.INDEX_SHARE, endOfIndexing, 1e-4f)
        val startOfExtraction = tracked(tracker, "Extracting", 0, 300).fraction
        assertTrue(startOfExtraction >= endOfIndexing)
        val middle = tracked(tracker, "Extracting", 150, 300).fraction
        assertTrue(middle > startOfExtraction)
        assertEquals(1f, tracked(tracker, "Completed", 300, 300).fraction, 1e-4f)
    }

    @Test
    fun pausedDuringIndexingKeepsTheIndexingScale() {
        val tracker = UnifiedProgressTracker()
        val before = tracked(tracker, "Indexing", 500, 1000).fraction
        val paused = tracked(tracker, "Paused", 500, 1000).fraction
        assertEquals(before, paused, 1e-4f)
    }

    @Test
    fun reopenedPausedExtractionUsesExtractionScale() {
        val state = tracked(UnifiedProgressTracker(), "Paused", 150, 300, stored = 150)
        assertEquals(UnifiedProgress.INDEX_SHARE + (1f - UnifiedProgress.INDEX_SHARE) * 0.5f, state.fraction, 1e-4f)
    }
}
