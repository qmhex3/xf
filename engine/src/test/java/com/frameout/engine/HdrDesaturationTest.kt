package com.frameout.engine

import android.media.MediaFormat
import com.frameout.engine.decode.ColorParams
import com.frameout.engine.decode.HdrTables
import com.frameout.engine.decode.HdrToneMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HdrDesaturationTest {
    private fun params() = ColorParams(true, true, MediaFormat.COLOR_TRANSFER_ST2084, 1000f)
    private fun plain() = HdrToneMapper(params(), 0f)
    private fun default() = HdrToneMapper(params())
    private fun red(argb: Int) = (argb shr 16) and 255
    private fun green(argb: Int) = (argb shr 8) and 255
    private fun blue(argb: Int) = argb and 255

    @Test
    fun weightIsZeroUpToTheKneeAndRisesToTheStrength() {
        val mapper = default()
        assertEquals(0f, mapper.desaturation(0.2f), 0f)
        assertEquals(0f, mapper.desaturation(0.75f), 0f)
        var last = 0f
        for (i in 0..100) {
            val w = mapper.desaturation(0.75f + i * 0.05f)
            assertTrue(w >= last)
            last = w
        }
        assertEquals(HdrTables.DESAT_STRENGTH, mapper.desaturation(1000f / 203f), 1e-4f)
        assertEquals(HdrTables.DESAT_STRENGTH, mapper.desaturation(50f), 1e-4f)
    }

    @Test
    fun diffuseWhiteIsBarelyTouched() {
        assertTrue(default().desaturation(1f) < 0.02f)
    }

    @Test
    fun midtonesAreIdenticalWithAndWithoutDesaturation() {
        listOf(floatArrayOf(0.5f, 0.4f, 0.3f), floatArrayOf(0.45f, 0.5f, 0.2f), floatArrayOf(0.3f, 0.35f, 0.5f)).forEach {
            assertEquals(plain().map(it[0], it[1], it[2]), default().map(it[0], it[1], it[2]))
        }
    }

    @Test
    fun brightYellowLosesSaturationButKeepsItsHue() {
        val before = plain().map(0.58f, 0.58f, 0.38f)
        val after = HdrToneMapper(params(), 1f).map(0.58f, 0.58f, 0.38f)
        assertTrue(blue(after) >= blue(before) + 8)
        assertTrue(red(after) <= red(before))
        assertTrue(green(after) <= green(before))
        assertTrue(red(after) > blue(after))
        assertTrue(green(after) > blue(after))
    }

    @Test
    fun defaultStrengthIsGentlerThanFullStrength() {
        val full = HdrToneMapper(params(), 1f).map(0.58f, 0.58f, 0.38f)
        val gentle = default().map(0.58f, 0.58f, 0.38f)
        val none = plain().map(0.58f, 0.58f, 0.38f)
        assertTrue(blue(gentle) >= blue(none))
        assertTrue(blue(gentle) <= blue(full))
    }

    @Test
    fun zeroStrengthDisablesTheStage() {
        val mapper = plain()
        for (i in 0..20) assertEquals(0f, mapper.desaturation(0.75f + i * 0.4f), 0f)
    }
}
