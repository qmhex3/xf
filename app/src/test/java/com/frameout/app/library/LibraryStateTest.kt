package com.frameout.app.library

import com.frameout.core.db.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryStateTest {
    private fun project(id: Long, name: String, updated: Long, frames: Int) =
        ProjectEntity(id = id, name = name, sourceUri = "", sourceName = "", extractedCount = frames, createdAt = 0, updatedAt = updated)

    private val state = LibraryState(
        projects = listOf(project(1, "beach", 10, 5), project(2, "Album", 30, 50), project(3, "city walk", 20, 500))
    )

    @Test
    fun newestFirstIsDefault() {
        assertEquals(listOf(2L, 3L, 1L), state.visibleProjects.map { it.id })
    }

    @Test
    fun oldestNameAndLargestOrderings() {
        assertEquals(listOf(1L, 3L, 2L), state.copy(projectSort = LibrarySort.Oldest).visibleProjects.map { it.id })
        assertEquals(listOf(2L, 1L, 3L), state.copy(projectSort = LibrarySort.Name).visibleProjects.map { it.id })
        assertEquals(listOf(3L, 2L, 1L), state.copy(projectSort = LibrarySort.Largest).visibleProjects.map { it.id })
    }

    @Test
    fun searchIgnoresCaseAndSurroundingSpaces() {
        assertEquals(listOf(2L), state.copy(projectQuery = "  ALB ").visibleProjects.map { it.id })
        assertEquals(emptyList<Long>(), state.copy(projectQuery = "zzz").visibleProjects.map { it.id })
    }

    @Test
    fun searchDropsSelectedProjectsThatAreNoLongerVisible() {
        val selected = state.copy(selectedProjects = setOf(1L, 2L))
        assertEquals(setOf(2L), selected.withProjectQuery("alb").selectedProjects)
    }

    @Test
    fun refreshedProjectListDropsDeletedSelections() {
        val selected = state.copy(selectedProjects = setOf(1L, 2L, 99L))
        assertEquals(setOf(1L, 2L), selected.withProjects(state.projects).selectedProjects)
    }

    @Test
    fun errorFlagFollowsTheMessageText() {
        val failed = state.withMessage("boom", true)
        assertTrue(failed.messageIsError)
        assertFalse(failed.withMessage("ok", false).messageIsError)
        assertFalse(failed.copy(message = "other").messageIsError)
        assertFalse(failed.copy(message = null).messageIsError)
    }
}
