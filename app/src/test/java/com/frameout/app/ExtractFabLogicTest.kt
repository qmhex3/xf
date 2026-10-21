package com.frameout.app

import com.frameout.app.library.FabPhase
import com.frameout.app.library.FabProject
import com.frameout.app.library.fabModel
import com.frameout.app.library.justCompleted
import com.frameout.app.library.pickFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractFabLogicTest {
    private fun p(id: Long, status: String, extracted: Int = 0, total: Int = 0) = FabProject(id, status, extracted, total)

    @Test
    fun idleLibraryShowsAdd() {
        val list = listOf(p(1, "Completed", 10, 10), p(2, "Cancelled"))
        assertNull(pickFocus(list, null))
        assertEquals(FabPhase.Add, fabModel(list, null, false).phase)
    }

    @Test
    fun oldPausedProjectDoesNotHijackFab() {
        assertNull(pickFocus(listOf(p(1, "Paused", 5, 10)), null))
    }

    @Test
    fun runningProjectShowsPauseWithProgress() {
        val list = listOf(p(1, "Extracting", 25, 100))
        val model = fabModel(list, pickFocus(list, null), false)
        assertEquals(FabPhase.Pause, model.phase)
        assertEquals(0.25f, model.progress!!, 0.001f)
    }

    @Test
    fun indexingAndUnknownTotalAreIndeterminate() {
        assertNull(fabModel(listOf(p(1, "Indexing", 0, 0)), 1, false).progress)
        assertNull(fabModel(listOf(p(1, "Extracting", 4, 0)), 1, false).progress)
    }

    @Test
    fun pausedFocusedProjectShowsResumeAndKeepsProgress() {
        val list = listOf(p(1, "Paused", 50, 100))
        assertEquals(1L, pickFocus(list, 1))
        val model = fabModel(list, 1, false)
        assertEquals(FabPhase.Resume, model.phase)
        assertEquals(0.5f, model.progress!!, 0.001f)
    }

    @Test
    fun liveProjectBeatsQueuedInBatch() {
        val list = listOf(p(3, "Queued"), p(2, "Extracting", 1, 10), p(1, "Completed"))
        assertEquals(2L, pickFocus(list, null))
    }

    @Test
    fun focusStaysOnRunningProject() {
        val list = listOf(p(2, "Extracting", 1, 10), p(1, "Extracting", 5, 10))
        assertEquals(1L, pickFocus(list, 1))
    }

    @Test
    fun completionFlashesOnceThenReturnsToAdd() {
        val done = listOf(p(1, "Completed", 10, 10))
        assertTrue(justCompleted(done, 1))
        assertEquals(FabPhase.Success, fabModel(done, 1, true).phase)
        assertNull(pickFocus(done, 1))
        assertEquals(FabPhase.Add, fabModel(done, null, false).phase)
    }

    @Test
    fun cancelledProjectIsNotACompletion() {
        assertEquals(false, justCompleted(listOf(p(1, "Cancelled")), 1))
    }
}
