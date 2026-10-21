package com.frameout.core.edit

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterPresetsTest {
    private fun apply(m: FloatArray, r: Float, g: Float, b: Float) = Triple(
        m[0] * r + m[1] * g + m[2] * b + m[4],
        m[5] * r + m[6] * g + m[7] * b + m[9],
        m[10] * r + m[11] * g + m[12] * b + m[14]
    )

    @Test
    fun idsAreUniqueAndMatricesAreComplete() {
        assertEquals(FilterPresets.All.size, FilterPresets.All.map { it.id }.toSet().size)
        FilterPresets.All.forEach { assertEquals(20, it.matrix.size) }
        assertTrue(FilterPresets.All.first().isNone)
    }

    @Test
    fun noFilterKeepsAdjustMatrixUnchanged() {
        assertArrayEquals(
            AdjustMatrix.values(0.1f, 1.2f, 0.9f),
            AdjustMatrix.values(0.1f, 1.2f, 0.9f, FilterPresets.NONE, 1f),
            1e-5f
        )
    }

    @Test
    fun monoProducesGray() {
        val (r, g, b) = apply(FilterPresets.matrix("mono", 1f), 200f, 100f, 50f)
        assertEquals(r, g, 1e-3f)
        assertEquals(g, b, 1e-3f)
    }

    @Test
    fun zeroStrengthIsIdentity() {
        FilterPresets.All.forEach { assertArrayEquals(ColorOps.identity(), FilterPresets.matrix(it.id, 0f), 1e-5f) }
    }

    @Test
    fun halfStrengthIsBetweenOriginalAndFull() {
        val (r, _, _) = apply(FilterPresets.matrix("sepia", 0.5f), 200f, 100f, 50f)
        val (full, _, _) = apply(FilterPresets.matrix("sepia", 1f), 200f, 100f, 50f)
        assertEquals((200f + full) / 2f, r, 1e-2f)
    }

    @Test
    fun unknownIdFallsBackToOriginal() {
        assertTrue(FilterPresets.find("does-not-exist").isNone)
        assertArrayEquals(ColorOps.identity(), FilterPresets.matrix("does-not-exist", 1f), 1e-5f)
    }

    @Test
    fun filterThenAdjustComposes() {
        val (r, g, b) = apply(AdjustMatrix.values(0f, 1f, 0f, "mono", 1f), 200f, 100f, 50f)
        assertEquals(r, g, 1e-3f)
        assertEquals(g, b, 1e-3f)
    }

    @Test
    fun filterCountsAsAdjustForRendering() {
        assertFalse(FrameEdit(1).hasAdjust)
        assertTrue(FrameEdit(1, filter = "noir").hasAdjust)
        assertFalse(FrameEdit(1, filter = "noir").isIdentity)
        assertFalse(FrameEdit(1, filter = "noir", filterStrength = 0f).hasAdjust)
    }
}
