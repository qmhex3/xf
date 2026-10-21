package com.frameout.core.edit

import org.junit.Assert.assertEquals
import org.junit.Test

class CropRatioTest {
    private fun pixelRatio(rect: CropRect, w: Int, h: Int): Float = ((rect.r - rect.l) * w) / ((rect.b - rect.t) * h)

    @Test
    fun squareInLandscapeKeepsFullHeight() {
        val rect = CropRatio.centered(CropPreset.Square, 1920, 1080)
        assertEquals(0f, rect.t, 1e-6f)
        assertEquals(1f, rect.b, 1e-6f)
        assertEquals(1f, pixelRatio(rect, 1920, 1080), 1e-4f)
    }

    @Test
    fun wideInPortraitKeepsFullWidth() {
        val rect = CropRatio.centered(CropPreset.Wide, 1080, 1920)
        assertEquals(0f, rect.l, 1e-6f)
        assertEquals(1f, rect.r, 1e-6f)
        assertEquals(16f / 9f, pixelRatio(rect, 1080, 1920), 1e-3f)
    }

    @Test
    fun rectIsCentered() {
        val rect = CropRatio.centered(CropPreset.Story, 1920, 1080)
        assertEquals(rect.l, 1f - rect.r, 1e-6f)
        assertEquals(rect.t, 1f - rect.b, 1e-6f)
    }

    @Test
    fun sameRatioReturnsFullFrame() {
        val rect = CropRatio.centered(CropPreset.Wide, 1920, 1080)
        assertEquals(CropRect(0f, 0f, 1f, 1f), rect)
    }

    @Test
    fun invalidSizeFallsBackToFullFrame() {
        assertEquals(CropRect(0f, 0f, 1f, 1f), CropRatio.centered(CropPreset.Square, 0, 0))
    }

    @Test
    fun clampKeepsMinimumSideInsideImage() {
        val rect = CropRatio.clamp(0.9f, 0.5f, 0.92f, 0.51f)
        assert(rect.r - rect.l >= 0.05f - 1e-6f)
        assert(rect.b - rect.t >= 0.05f - 1e-6f)
        assert(rect.r <= 1f && rect.b <= 1f && rect.l >= 0f && rect.t >= 0f)
    }

    @Test
    fun clampLeavesValidRectUntouched() {
        val rect = CropRatio.clamp(0.1f, 0.2f, 0.8f, 0.9f)
        assert(rect == CropRect(0.1f, 0.2f, 0.8f, 0.9f))
    }
}
