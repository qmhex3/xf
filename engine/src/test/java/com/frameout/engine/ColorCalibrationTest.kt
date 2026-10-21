package com.frameout.engine

import android.media.MediaFormat
import com.frameout.engine.decode.CalibrationRule
import com.frameout.engine.decode.ColorParams
import com.frameout.engine.decode.ColorVariants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCalibrationTest {
    private val hdr = ColorParams(true, true, MediaFormat.COLOR_TRANSFER_ST2084, 1000f, primaries = 1)

    @Test
    fun keepsMetadataReadingWhenItAlreadyMatches() {
        assertEquals(ColorVariants.AUTO, CalibrationRule.pick(3f, emptyMap()))
        assertEquals(ColorVariants.AUTO, CalibrationRule.pick(8f, mapOf(2 to 7f)))
    }

    @Test
    fun switchesWhenAVariantClearlyMatchesTheSystemFrame() {
        assertEquals(2, CalibrationRule.pick(30f, mapOf(2 to 5f, 3 to 20f)))
        assertEquals(2, CalibrationRule.pick(6f, mapOf(2 to 1f)))
    }

    @Test
    fun refusesToDecideWhenFramesAreNotComparable() {
        assertNull(CalibrationRule.pick(70f, mapOf(2 to 65f)))
    }

    @Test
    fun meanAbsDifference() {
        assertEquals(4f, CalibrationRule.meanAbsDifference(floatArrayOf(0f, 10f), floatArrayOf(4f, 6f)), 0f)
        assertTrue(CalibrationRule.meanAbsDifference(floatArrayOf(), floatArrayOf()).isNaN())
    }

    @Test
    fun sdrVariantsNeverToneMapOrConvertGamut() {
        assertTrue(hdr.hdr && hdr.gamutConvert.not())
        for (id in 1..8) {
            val v = ColorVariants.apply(id, hdr)
            assertFalse("variant $id", v.hdr)
            assertFalse("variant $id", v.gamutConvert)
        }
    }

    @Test
    fun variantsChangeOnlyWhatTheyClaim() {
        assertSame(hdr, ColorVariants.apply(ColorVariants.AUTO, hdr))
        assertTrue(ColorVariants.apply(3, hdr).bt601)
        assertFalse(ColorVariants.apply(6, hdr).limited)
        val inverted = ColorVariants.apply(9, hdr)
        assertTrue(inverted.hdr && inverted.bt2020 && !inverted.limited)
        assertEquals((1..9).toSet(), ColorVariants.CANDIDATES.toSet())
    }
}
