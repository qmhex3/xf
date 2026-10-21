package com.frameout.core.storage

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class ExifTextTest {
    @Test
    fun formatsDateTimeInExifLayout() {
        assertEquals("2024:03:05 07:08:09", ExifText.dateTime(1709622489000L, TimeZone.getTimeZone("UTC")))
    }

    @Test
    fun subSecondIsThreeDigits() {
        assertEquals("007", ExifText.subSecond(1709622489007L))
        assertEquals("000", ExifText.subSecond(1709622489000L))
    }

    @Test
    fun positionHandlesMinutesAndHours() {
        assertEquals("00:01.500", ExifText.position(1_500_000L))
        assertEquals("1:02:03.004", ExifText.position(3_723_004_000L))
    }

    @Test
    fun asciiKeepsPrintableAndReplacesTheRest() {
        assertEquals("Trip - #1 - 00:01.000", ExifText.ascii("Trip \u00B7 #1 \u00B7 00:01.000"))
        assertEquals("a?b", ExifText.ascii("a\u0645b"))
    }

    @Test
    fun descriptionIncludesNameIndexAndTime() {
        assertEquals("Trip · #12 · 00:02.000", ExifText.description("Trip", 12, 2_000_000L))
    }
}
