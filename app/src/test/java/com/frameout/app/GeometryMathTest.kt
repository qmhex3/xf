package com.frameout.app

import com.frameout.app.viewer.GeometryMath
import com.frameout.core.edit.CropRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryMathTest {
    private val rect = CropRect(0.1f, 0.2f, 0.6f, 0.9f)

    private fun assertRect(expected: CropRect, actual: CropRect) {
        assertEquals(expected.l, actual.l, 1e-5f)
        assertEquals(expected.t, actual.t, 1e-5f)
        assertEquals(expected.r, actual.r, 1e-5f)
        assertEquals(expected.b, actual.b, 1e-5f)
    }

    @Test
    fun roundTripsForEveryRotationAndFlip() {
        for (rotation in listOf(0, 90, 180, 270)) for (h in listOf(false, true)) for (v in listOf(false, true)) {
            val shown = GeometryMath.sourceToDisplay(rect, rotation, h, v)
            assertRect(rect, GeometryMath.displayToSource(shown, rotation, h, v))
        }
    }

    @Test
    fun quarterTurnMovesTopLeftCornerToTopRight() {
        val shown = GeometryMath.sourceToDisplay(CropRect(0f, 0f, 0.25f, 0.5f), 90, false, false)
        assertRect(CropRect(0.5f, 0f, 1f, 0.25f), shown)
    }

    @Test
    fun fullFrameStaysFullFrame() {
        for (rotation in listOf(0, 90, 180, 270)) {
            assertRect(CropRect(0f, 0f, 1f, 1f), GeometryMath.displayToSource(CropRect(0f, 0f, 1f, 1f), rotation, true, false))
        }
    }

    @Test
    fun displayHorizontalFlipSwapsAxisWhenTurnedSideways() {
        assertEquals(true to false, GeometryMath.toggleDisplayFlipH(0, false, false))
        assertEquals(false to true, GeometryMath.toggleDisplayFlipH(90, false, false))
        assertEquals(true to false, GeometryMath.toggleDisplayFlipH(180, false, false))
        assertEquals(false to true, GeometryMath.toggleDisplayFlipH(270, false, false))
        assertEquals(false to true, GeometryMath.toggleDisplayFlipV(0, false, false))
        assertEquals(true to false, GeometryMath.toggleDisplayFlipV(90, false, false))
    }

    @Test
    fun togglingTwiceRestoresTheState() {
        for (rotation in listOf(0, 90, 180, 270)) {
            val once = GeometryMath.toggleDisplayFlipH(rotation, false, true)
            assertEquals(false to true, GeometryMath.toggleDisplayFlipH(rotation, once.first, once.second))
        }
    }

    @Test
    fun normalizesNegativeAndLargeAngles() {
        assertEquals(270, GeometryMath.normalize(-90))
        assertEquals(90, GeometryMath.normalize(450))
        assertTrue(GeometryMath.swaps(270))
        assertFalse(GeometryMath.swaps(180))
    }
}
