package com.frameout.core

import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectEntity
import com.frameout.core.db.SavedImageEntity
import com.frameout.core.db.toModel
import com.frameout.core.model.ProjectStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {
    @Test
    fun projectEntityToModel() {
        val entity = ProjectEntity(
            id = 1,
            name = "Test",
            sourceUri = "content://x",
            sourceName = "v.mp4",
            durationMs = 1000,
            frameCount = 30,
            extractedCount = 10,
            lastPtsUs = 500000,
            lastSampleIndex = 15,
            status = "Extracting",
            createdAt = 1,
            updatedAt = 2,
            thumbnailPath = null
        )
        val model = entity.toModel()
        assertEquals(1L, model.id)
        assertEquals("Test", model.name)
        assertEquals(ProjectStatus.Extracting, model.status)
        assertEquals(10, model.extractedCount)
        assertEquals(15, model.lastSampleIndex)
    }

    @Test
    fun unknownStatusDefaultsToIdle() {
        val entity = ProjectEntity(
            id = 2,
            name = "A",
            sourceUri = "u",
            sourceName = "n",
            status = "Nope",
            createdAt = 0,
            updatedAt = 0
        )
        assertEquals(ProjectStatus.Idle, entity.toModel().status)
    }

    @Test
    fun frameEntityToModel() {
        val entity = FrameEntity(
            id = 5,
            projectId = 1,
            index = 3,
            ptsUs = 100000,
            isKeyframe = true,
            isFavorite = false,
            isDeleted = false
        )
        val model = entity.toModel()
        assertEquals(5L, model.id)
        assertEquals(3, model.index)
        assertTrue(model.isKeyframe)
        assertFalse(model.isFavorite)
    }

    @Test
    fun savedImageEntityToModel() {
        val entity = SavedImageEntity(
            id = 9,
            uri = "content://img",
            displayName = "frame.jpg",
            album = "Frame out",
            width = 1920,
            height = 1080,
            createdAt = 99,
            sourceFrameId = 5
        )
        val model = entity.toModel()
        assertEquals("frame.jpg", model.displayName)
        assertEquals(1920, model.width)
        assertEquals(5L, model.sourceFrameId)
    }
}
