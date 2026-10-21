package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CropRulesTest {
    @Test
    fun cropBelowTheMinimumIsRejected() {
        val box = NormBox(0.1f, 0.1f, 0.2f, 0.3f)
        assertNull(CropRules.rect(box, 640, 480))
    }

    @Test
    fun sameBoxPassesOnAHigherResolutionImage() {
        val box = NormBox(0.1f, 0.1f, 0.3f, 0.4f)
        assertNull(CropRules.rect(box, 400, 300))
        assertNotNull(CropRules.rect(box, 1024, 768))
    }

    @Test
    fun bothSidesMustReachTheMinimum() {
        val tall = NormBox(0.1f, 0.0f, 0.12f, 1.0f)
        assertNull(CropRules.rect(tall, 1024, 576))
        val wide = NormBox(0.0f, 0.1f, 1.0f, 0.12f)
        assertNull(CropRules.rect(wide, 1024, 576))
    }

    @Test
    fun rectangleMatchesThePixelBox() {
        val rect = CropRules.rect(NormBox(0.25f, 0.5f, 0.75f, 1.0f), 1000, 800)!!
        assertEquals(250, rect.x)
        assertEquals(400, rect.y)
        assertEquals(500, rect.width)
        assertEquals(400, rect.height)
    }

    @Test
    fun rectangleStaysInsideTheImage() {
        val rect = CropRules.rect(NormBox(0.9f, 0.9f, 1.0f, 1.0f), 1000, 1000, minSide = 10)!!
        assertEquals(1000, rect.x + rect.width)
        assertEquals(1000, rect.y + rect.height)
    }

    @Test
    fun customMinimumIsHonoured() {
        val box = NormBox(0.0f, 0.0f, 0.1f, 0.1f)
        assertNotNull(CropRules.rect(box, 800, 800, minSide = 64))
        assertNull(CropRules.rect(box, 800, 800, minSide = 96))
    }

    @Test
    fun emptyImageGivesNoCrop() {
        assertNull(CropRules.rect(NormBox(0f, 0f, 1f, 1f), 0, 100))
    }
}
