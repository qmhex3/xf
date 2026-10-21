package com.frameout.app

import com.frameout.app.trim.TimelineZoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineZoomTest {
    private val duration = 60_000L

    @Test
    fun startsShowingTheWholeClip() {
        val zoom = TimelineZoom()
        assertFalse(zoom.zoomed)
        assertEquals(0f, zoom.start, 0f)
        assertEquals(1f, zoom.span, 0f)
    }

    @Test
    fun zoomKeepsTheMomentUnderTheFingersFixed() {
        val zoom = TimelineZoom().transform(0.25f, 0f, 2f, duration)
        assertEquals(2f, zoom.scale, 0.0001f)
        assertEquals(0.25f, zoom.start + 0.25f * zoom.span, 0.0001f)
    }

    @Test
    fun scaleIsCappedByClipLength() {
        assertEquals(TimelineZoom.MAX_SCALE, TimelineZoom().transform(0.5f, 0f, 1000f, duration).scale, 0f)
        assertEquals(3f, TimelineZoom().transform(0.5f, 0f, 100f, 3_000L).scale, 0f)
        assertFalse(TimelineZoom().transform(0.5f, 0f, 100f, 800L).zoomed)
    }

    @Test
    fun pinchingBackInResetsToTheWholeClip() {
        val zoomed = TimelineZoom().transform(0.5f, 0f, 4f, duration)
        assertEquals(TimelineZoom(), zoomed.transform(0.5f, 0f, 0.01f, duration))
    }

    @Test
    fun dragIsClampedInsideTheClip() {
        val zoomed = TimelineZoom().transform(0.5f, 0f, 4f, duration)
        assertEquals(0f, zoomed.transform(0.5f, 50f, 1f, duration).start, 0f)
        assertEquals(0.75f, zoomed.transform(0.5f, -50f, 1f, duration).start, 0.0001f)
    }

    @Test
    fun movingFingersRightRevealsEarlierTime() {
        val zoomed = TimelineZoom().transform(0.5f, 0f, 4f, duration)
        assertTrue(zoomed.transform(0.5f, 0.1f, 1f, duration).start < zoomed.start)
    }

    @Test
    fun edgeScrollMovesByAFractionOfTheWindowAndStopsAtTheEnds() {
        val zoomed = TimelineZoom().transform(0.5f, 0f, 4f, duration)
        assertEquals(0.5f, zoomed.scrolledBy(0.5f).start, 0.0001f)
        assertEquals(0.75f, zoomed.scrolledBy(100f).start, 0.0001f)
        assertEquals(0f, zoomed.scrolledBy(-100f).start, 0f)
        assertEquals(TimelineZoom(), TimelineZoom().scrolledBy(1f))
    }

    @Test
    fun revealingOnlyMovesWhenThePointIsOutside() {
        val zoomed = TimelineZoom().transform(0.5f, 0f, 4f, duration)
        assertSame(zoomed, zoomed.revealing(30_000L, duration))
        val moved = zoomed.revealing(5_000L, duration)
        assertTrue(5_000f / duration in moved.start..moved.end)
        assertFalse(TimelineZoom().revealing(5_000L, duration).zoomed)
    }
}
