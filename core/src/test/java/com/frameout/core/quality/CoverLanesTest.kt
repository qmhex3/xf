package com.frameout.core.quality

import com.frameout.core.db.FrameEntity
import com.frameout.core.db.FrameTagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverLanesTest {
    private fun frame(id: Long) = FrameEntity(id = id, projectId = 1, index = id.toInt(), ptsUs = id * 1000L)
    private fun label(frameId: Long, name: String, conf: Float = 0.9f) = FrameTagEntity(frameId = frameId, projectId = 1, tag = TagRules.LABEL + name, confidence = conf)

    @Test
    fun everyVehicleKindGetsItsOwnLaneEvenWhenCarsDominate() {
        val frames = (1L..12L).map(::frame)
        val labels = (1L..8L).map { label(it, "Car") } + (9L..10L).map { label(it, "Airliner") } + label(11, "Bicycle") + label(12, "Sailboat")
        val lanes = CoverLanes.build("f:vehicles", frames, labels)
        assertEquals(listOf("Car", "Airplane", "Bicycle", "Boat"), lanes.map { it.kind })
    }

    @Test
    fun synonymsMergeIntoOneKind() {
        val frames = (1L..3L).map(::frame)
        val labels = listOf(label(1, "Airplane"), label(2, "Aircraft"), label(3, "Aviation"), label(1, "Vehicle"), label(4, "Car"))
        val lanes = CoverLanes.build("f:vehicles", frames, labels + label(4, "Bicycle"))
        assertTrue(lanes.none { it.kind == "Aircraft" || it.kind == "Aviation" })
    }

    @Test
    fun albumWithOneKindFallsBackToEvenChunks() {
        val frames = (1L..20L).map(::frame)
        val lanes = CoverLanes.build("f:vehicles", frames, frames.map { label(it.id, "Car") })
        assertEquals(4, lanes.size)
        assertTrue(lanes.all { it.kind == null && it.frames.isNotEmpty() })
    }

    @Test
    fun sequencePassesThroughEveryLaneInTurn() {
        val frames = (1L..12L).map(::frame)
        val labels = (1L..6L).map { label(it, "Car") } + (7L..12L).map { label(it, "Airplane") }
        val lanes = CoverLanes.build("f:vehicles", frames, labels)
        assertEquals(listOf(0, 1, 0, 1), CoverLanes.sequence(lanes).take(4).map { it.first })
    }

    @Test
    fun leisureAndFunAreNoLongerUnmapped() {
        assertEquals("events", TagFamilies.familyOfLabel("Leisure"))
        assertEquals("events", TagFamilies.familyOfLabel("Fun"))
    }
}
