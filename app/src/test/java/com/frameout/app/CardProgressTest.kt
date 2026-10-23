package com.frameout.app

import com.frameout.app.library.CardProgress
import com.frameout.app.library.cardProgress
import com.frameout.core.db.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardProgressTest {
    private fun project(status: String, extracted: Int = 0, total: Int = 0) = ProjectEntity(
        name = "p", sourceUri = "u", sourceName = "n", createdAt = 0L, updatedAt = 0L,
        status = status, extractedCount = extracted, frameCount = total
    )

    @Test
    fun indexingWithoutLiveValueIsIndeterminate() {
        assertEquals(CardProgress.Indeterminate, cardProgress(project("Indexing", 0, 155_082)))
    }

    @Test
    fun indexingWithLiveValueShowsItAndStaysInsideTheIndexShare() {
        val shown = cardProgress(project("Indexing", 0, 155_082), 0.04f) as CardProgress.Value
        assertEquals(4, shown.percent)
        assertTrue(shown.fraction <= 0.2f)
    }

    @Test
    fun extractionStartsAtTheIndexShareNotAtZero() {
        val shown = cardProgress(project("Extracting", 1, 1000)) as CardProgress.Value
        assertTrue(shown.fraction >= 0.2f)
        val half = cardProgress(project("Extracting", 500, 1000)) as CardProgress.Value
        assertEquals(60, half.percent)
    }

    @Test
    fun liveValueNeverMakesTheCardGoBackwards() {
        val shown = cardProgress(project("Extracting", 500, 1000), 0.3f) as CardProgress.Value
        assertEquals(60, shown.percent)
    }

    @Test
    fun pausedKeepsItsPositionAndCompletedHasNoBar() {
        val paused = cardProgress(project("Paused", 500, 1000)) as CardProgress.Value
        assertEquals(60, paused.percent)
        assertEquals(CardProgress.None, cardProgress(project("Completed", 1000, 1000)))
    }
}
