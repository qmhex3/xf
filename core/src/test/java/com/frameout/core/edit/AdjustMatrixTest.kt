package com.frameout.core.edit

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AdjustMatrixTest {
    private fun apply(m: FloatArray, r: Float, g: Float, b: Float): Triple<Float, Float, Float> = Triple(
        m[0] * r + m[1] * g + m[2] * b + m[4],
        m[5] * r + m[6] * g + m[7] * b + m[9],
        m[10] * r + m[11] * g + m[12] * b + m[14]
    )

    @Test
    fun neutralValuesKeepColorsUntouched() {
        val m = AdjustMatrix.values(0f, 1f, 1f)
        val expected = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        assertArrayEquals(expected, m, 1e-5f)
    }

    @Test
    fun zeroSaturationProducesGray() {
        val (r, g, b) = apply(AdjustMatrix.values(0f, 1f, 0f), 200f, 100f, 50f)
        assertEquals(r, g, 1e-3f)
        assertEquals(g, b, 1e-3f)
    }

    @Test
    fun brightnessShiftsEveryChannelEqually() {
        val (r, g, b) = apply(AdjustMatrix.values(0.2f, 1f, 1f), 10f, 20f, 30f)
        assertEquals(61f, r, 1e-3f)
        assertEquals(71f, g, 1e-3f)
        assertEquals(81f, b, 1e-3f)
    }

    @Test
    fun contrastKeepsMidGrayFixed() {
        val (r, g, b) = apply(AdjustMatrix.values(0f, 1.4f, 1f), 127.5f, 127.5f, 127.5f)
        assertEquals(127.5f, r, 1e-2f)
        assertEquals(127.5f, g, 1e-2f)
        assertEquals(127.5f, b, 1e-2f)
    }

    @Test
    fun alphaRowIsIdentity() {
        val m = AdjustMatrix.values(0.3f, 0.7f, 1.6f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 1f, 0f), m.copyOfRange(15, 20), 0f)
    }
}
