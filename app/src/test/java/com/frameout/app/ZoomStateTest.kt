package com.frameout.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.frameout.app.trim.ZoomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ZoomStateTest {
    private val size = Size(400f, 225f)
    private val center = Offset(200f, 112.5f)

    @Test
    fun startsUnzoomed() {
        assertFalse(ZoomState().zoomed)
    }

    @Test
    fun scaleIsClampedToLimits() {
        val high = ZoomState().transform(center, Offset.Zero, 100f, size)
        assertEquals(ZoomState.MAX_SCALE, high.scale, 0f)
        val low = high.transform(center, Offset.Zero, 0.0001f, size)
        assertEquals(ZoomState(), low)
    }

    @Test
    fun panStaysInsideBounds() {
        val zoomed = ZoomState().transform(center, Offset.Zero, 2f, size)
        val dragged = zoomed.transform(center, Offset(5000f, -5000f), 1f, size)
        assertEquals(200f, dragged.offset.x, 0.001f)
        assertEquals(-112.5f, dragged.offset.y, 0.001f)
    }

    @Test
    fun zoomKeepsPointUnderFingerFixed() {
        val point = Offset(300f, 60f)
        val zoomed = ZoomState().transform(point, Offset.Zero, 2f, size)
        val mapped = center + (point - center) * zoomed.scale + zoomed.offset
        assertEquals(point.x, mapped.x, 0.001f)
        assertEquals(point.y, mapped.y, 0.001f)
    }

    @Test
    fun relaxedBlendsBackToIdentity() {
        val zoomed = ZoomState().transform(Offset(300f, 60f), Offset.Zero, 4f, size)
        val half = zoomed.relaxed(0.5f)
        assertEquals(2.5f, half.scale, 0.001f)
        assertEquals(zoomed.offset.x * 0.5f, half.offset.x, 0.001f)
        assertEquals(zoomed, zoomed.relaxed(0f))
        assertEquals(ZoomState(), zoomed.relaxed(1f))
    }
}
