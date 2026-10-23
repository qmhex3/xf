package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartZoomTest {
    @Test
    fun gridCoversTheFrameWithOverlap() {
        val g = SmartZoom.grid(1024, 576)
        assertEquals(4, g.size)
        assertEquals(0, g.minOf { it.x })
        assertEquals(1024, g.maxOf { it.r })
        assertEquals(0, g.minOf { it.y })
        assertEquals(576, g.maxOf { it.b })
        assertTrue(g.first { it.x == 0 }.r > g.first { it.x > 0 }.x)
    }

    @Test
    fun tinyFramesAreNotTiled() {
        assertTrue(SmartZoom.grid(256, 256).isEmpty())
        assertTrue(SmartZoom.grid(0, 100).isEmpty())
    }

    @Test
    fun scaleOnlyEnlarges() {
        assertEquals(1f, SmartZoom.scaleFor(800, 600), 0f)
        assertEquals(2f, SmartZoom.scaleFor(320, 200), 0.001f)
        assertEquals(3f, SmartZoom.scaleFor(100, 100), 0f)
    }

    @Test
    fun zoomWindowStaysInsideTheFrame() {
        val corner = SmartZoom.zoomRect(PxRect(0, 0, 40, 40), 1000, 600)!!
        assertEquals(0, corner.x)
        assertEquals(0, corner.y)
        assertEquals(80, corner.w)
        val middle = SmartZoom.zoomRect(PxRect(480, 280, 40, 40), 1000, 600)!!
        assertEquals(460, middle.x)
        assertEquals(260, middle.y)
        val edge = SmartZoom.zoomRect(PxRect(980, 580, 20, 20), 1000, 600)!!
        assertTrue(edge.r <= 1000 && edge.b <= 600)
        assertNull(SmartZoom.zoomRect(PxRect(1, 1, 2, 2), 1000, 600))
    }

    @Test
    fun boxTouchingAnInnerTileEdgeIsCut() {
        val tile = PxRect(400, 0, 600, 576)
        assertTrue(SmartZoom.cutByTile(PxRect(401, 100, 50, 50), tile, 1000, 576))
        assertFalse(SmartZoom.cutByTile(PxRect(500, 100, 50, 50), tile, 1000, 576))
        assertFalse(SmartZoom.cutByTile(PxRect(940, 100, 60, 50), tile, 1000, 576))
    }

    @Test
    fun duplicatesKeepTheLargerBox() {
        val a = PxRect(10, 10, 100, 100)
        val b = PxRect(15, 12, 90, 95)
        val far = PxRect(400, 400, 50, 50)
        assertEquals(listOf(a, far), SmartZoom.suppress(listOf(b, a, far), { it }))
        assertEquals(1, SmartZoom.suppress(listOf(a, PxRect(30, 30, 30, 30)), { it }).size)
    }

    @Test
    fun faceShapeIsChecked() {
        assertTrue(SmartZoom.plausibleFace(PxRect(0, 0, 80, 100)))
        assertFalse(SmartZoom.plausibleFace(PxRect(0, 0, 300, 40)))
    }

    @Test
    fun tileOnlyLabelNeedsConfidenceOrAgreement() {
        assertNull(SmartZoom.vote(null, listOf(0.7f)))
        assertEquals(0.8f, SmartZoom.vote(null, listOf(0.8f))!!, 0.001f)
        assertEquals(0.68f, SmartZoom.vote(null, listOf(0.7f, 0.66f))!!, 0.001f)
        assertNull(SmartZoom.vote(null, listOf(0.7f, 0.5f)))
    }

    @Test
    fun wholeFrameLabelIsKeptAndConfirmedByTiles() {
        assertEquals(0.65f, SmartZoom.vote(0.65f, listOf(0.5f))!!, 0.001f)
        assertEquals(0.9f, SmartZoom.vote(0.8f, listOf(0.85f))!!, 0.001f)
        assertEquals(1f, SmartZoom.vote(0.98f, listOf(0.99f))!!, 0.001f)
    }
}
