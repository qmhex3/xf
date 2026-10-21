package com.frameout.core.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewGeometryTest {
    private val geo = ViewGeometry(4000, 3000, 1000f, 1000f)

    @Test
    fun fitUsesShorterRatio() {
        assertEquals(0.25f, geo.fit, 1e-6f)
    }

    @Test
    fun maxScaleReachesFortyEightScreenPixelsPerImagePixel() {
        assertEquals(192f, geo.maxScale, 0.01f)
        assertEquals(48f, geo.pixelSize(Transform(geo.maxScale, 0f, 0f)), 0.01f)
    }

    @Test
    fun maxScaleIsCapped() {
        assertEquals(ViewGeometry.ABS_MAX_SCALE, ViewGeometry(40000, 30000, 100f, 100f).maxScale, 0f)
        assertEquals(ViewGeometry.MIN_MAX_SCALE, ViewGeometry(10, 10, 4000f, 4000f).maxScale, 0f)
    }

    @Test
    fun screenAndImageCoordinatesRoundTrip() {
        val t = Transform(7.5f, -120f, 85f)
        val sx = geo.toScreenX(0.31f, t)
        val sy = geo.toScreenY(0.77f, t)
        assertEquals(0.31f, geo.toNormX(sx, t), 1e-5f)
        assertEquals(0.77f, geo.toNormY(sy, t), 1e-5f)
    }

    @Test
    fun centerOfViewMapsToCenterOfImageAtIdentity() {
        assertEquals(0.5f, geo.toNormX(0f, Transform.Identity), 1e-6f)
        assertEquals(0.5f, geo.toNormY(0f, Transform.Identity), 1e-6f)
    }

    @Test
    fun zoomKeepsFocalPointStill() {
        val t = Transform(2f, 40f, -30f)
        val fx = 150f
        val fy = -90f
        val before = geo.toNormX(fx, t) to geo.toNormY(fy, t)
        val after = geo.zoomed(t, 3f, fx, fy)
        assertEquals(before.first, geo.toNormX(fx, after), 1e-4f)
        assertEquals(before.second, geo.toNormY(fy, after), 1e-4f)
    }

    @Test
    fun zoomClampsToLimits() {
        assertEquals(geo.maxScale, geo.zoomed(Transform.Identity, 1e6f, 0f, 0f).scale, 0f)
        assertEquals(1f, geo.zoomed(Transform(4f, 0f, 0f), 1e-6f, 0f, 0f).scale, 0f)
    }

    @Test
    fun panStopsAtImageEdges() {
        val t = geo.clamp(Transform(2f, 99999f, -99999f))
        val limitX = (4000 * 0.25f * 2f - 1000f) / 2f
        val limitY = (3000 * 0.25f * 2f - 1000f) / 2f
        assertEquals(limitX, t.x, 0.01f)
        assertEquals(-limitY, t.y, 0.01f)
    }

    @Test
    fun smallerAxisStaysCentered() {
        assertEquals(0f, geo.clamp(Transform(1f, 300f, 300f)).y, 0f)
    }

    @Test
    fun actualPixelsScaleShowsOneToOne() {
        assertTrue(geo.actualPixels() == 4f)
        assertEquals(1f, geo.pixelSize(Transform(geo.actualPixels(), 0f, 0f)), 1e-5f)
    }
}
