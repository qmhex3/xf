package com.frameout.core.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkCodecTest {
    @Test
    fun roundTripKeepsStrokesAndCovers() {
        val marks = listOf(
            StrokeMark(-65536, 0.0125f, listOf(MarkPoint(0.1f, 0.2f), MarkPoint(0.35f, 0.5f))),
            CoverMark(0.1f, 0.2f, 0.6f, 0.7f, CoverStyle.Blur),
            CoverMark(0f, 0f, 1f, 1f, CoverStyle.Solid)
        )
        assertEquals(marks, MarkCodec.decode(MarkCodec.encode(marks)))
    }

    @Test
    fun textMarkRoundTripKeepsUnicodeAndSeparators() {
        val marks = listOf<EditMark>(
            TextMark("مرحبا | سطر\nثاني; 100%", 0.25f, 0.75f, 0.1f, -1, bold = true, italic = true, font = 1,
                shadow = 0.5f, outline = 0.25f, outlineColor = -16777216, background = 0x80000000.toInt(), opacity = 0.8f, rotation = -15f),
            StrokeMark(-65536, 0.0125f, listOf(MarkPoint(0.1f, 0.2f)))
        )
        assertEquals(marks, MarkCodec.decode(MarkCodec.encode(marks)))
    }

    @Test
    fun emptyTextDecodesToNothing() {
        assertTrue(MarkCodec.decode("").isEmpty())
        assertTrue(MarkCodec.decode("   ").isEmpty())
        assertEquals("", MarkCodec.encode(emptyList()))
    }

    @Test
    fun malformedLinesAreSkipped() {
        val text = "garbage\nS|1|0.01|\nC|Nope|0,0,1,1\nC|Pixelate|0.1,0.1,0.5,0.5\nS|x|y|0,0"
        assertEquals(listOf<EditMark>(CoverMark(0.1f, 0.1f, 0.5f, 0.5f, CoverStyle.Pixelate)), MarkCodec.decode(text))
    }

    @Test
    fun valuesAreQuantizedToFiveDecimals() {
        val decoded = MarkCodec.decode(MarkCodec.encode(listOf(CoverMark(0.123456f, 0.2f, 0.3f, 0.4f, CoverStyle.Blur))))
        assertEquals(CoverMark(0.12346f, 0.2f, 0.3f, 0.4f, CoverStyle.Blur), decoded.single())
    }

    @Test
    fun brushCoverRoundTripsWithStrength() {
        val marks = listOf<EditMark>(BrushCoverMark(CoverStyle.Pixelate, 0.35f, 0.0123f, listOf(MarkPoint(0.1f, 0.2f), MarkPoint(0.3f, 0.4f))))
        assertEquals(marks, MarkCodec.decode(MarkCodec.encode(marks)))
    }

    @Test
    fun tinyValuesSurviveScientificNotation() {
        val marks = listOf<EditMark>(StrokeMark(-1, 0.0002f, listOf(MarkPoint(0.00001f, 0.99999f))))
        assertEquals(marks, MarkCodec.decode(MarkCodec.encode(marks)))
    }

    @Test
    fun singlePointStrokeSurvives() {
        val marks = listOf<EditMark>(StrokeMark(-16777216, 0.006f, listOf(MarkPoint(0.5f, 0.5f))))
        assertEquals(marks, MarkCodec.decode(MarkCodec.encode(marks)))
    }

    @Test
    fun legacyCoverWithoutStrengthGetsDefault() {
        val decoded = MarkCodec.decode("C|Blur|0.1,0.1,0.5,0.5")
        assertEquals(listOf<EditMark>(CoverMark(0.1f, 0.1f, 0.5f, 0.5f, CoverStyle.Blur, DEFAULT_COVER_STRENGTH)), decoded)
    }

    @Test
    fun malformedBrushLinesAreSkipped() {
        val text = "B|Blur|0.5|0.01|\nB|Solid|0.5\nB|Nope|0.5|0.01|0.1,0.1\nB|Solid|1|0.02|0.3,0.3"
        val expected = listOf<EditMark>(BrushCoverMark(CoverStyle.Solid, 1f, 0.02f, listOf(MarkPoint(0.3f, 0.3f))))
        assertEquals(expected, MarkCodec.decode(text))
    }
}
