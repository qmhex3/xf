package com.frameout.engine

import android.media.MediaFormat
import com.frameout.engine.decode.ColorParams
import com.frameout.engine.decode.HdrTables
import com.frameout.engine.decode.HdrToneMapper
import com.frameout.engine.decode.PeakNits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HdrPeakTest {
    private fun pq(peak: Float) = HdrToneMapper(ColorParams(true, true, MediaFormat.COLOR_TRANSFER_ST2084, peak))

    @Test
    fun pqKeepsPeaksAboveFourThousandNits() {
        assertEquals(10_000f, PeakNits.resolve(10_000, 0, false), 0.01f)
        assertEquals(6_000f, PeakNits.resolve(0, 6_000, false), 0.01f)
    }

    @Test
    fun pqClampsToPqLimitAndFloor() {
        assertEquals(10_000f, PeakNits.resolve(40_000, 0, false), 0.01f)
        assertEquals(400f, PeakNits.resolve(100, 0, false), 0.01f)
    }

    @Test
    fun missingMetadataFallsBackToThousand() {
        assertEquals(1000f, PeakNits.resolve(0, 0, false), 0.01f)
        assertEquals(1000f, PeakNits.resolve(0, 0, true), 0.01f)
    }

    @Test
    fun hlgHonorsMetadataWithinItsRange() {
        assertEquals(1500f, PeakNits.resolve(1500, 0, true), 0.01f)
        assertEquals(4000f, PeakNits.resolve(9000, 0, true), 0.01f)
    }

    @Test
    fun ootfAtThousandNitsMatchesTheReferenceTable() {
        val built = HdrTables.buildOotf(1000f)
        assertTrue(built.indices.all { built[it] == HdrTables.ootf[it] })
    }

    @Test
    fun brighterHlgReferenceRaisesTheCurve() {
        val bright = HdrTables.buildOotf(4000f)
        val reference = HdrTables.ootf
        assertTrue(bright[2048] > reference[2048])
        assertTrue(bright[4096] > reference[4096])
    }

    @Test
    fun highPeakContentKeepsHighlightSeparation() {
        val capped = pq(4_000f)
        val full = pq(10_000f)
        assertEquals(1f, capped.toneLuma(30f), 0f)
        assertEquals(1f, capped.toneLuma(49f), 0f)
        assertTrue(full.toneLuma(30f) < full.toneLuma(49f))
    }
}
