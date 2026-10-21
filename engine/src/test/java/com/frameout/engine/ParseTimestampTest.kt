package com.frameout.engine

import com.frameout.engine.timeline.parseTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseTimestampTest {
    @Test
    fun readsSecondsMinutesAndHours() {
        assertEquals(5_000L, parseTimestamp("5"))
        assertEquals(90_000L, parseTimestamp("1:30"))
        assertEquals(3_723_500L, parseTimestamp("1:02:03.5"))
        assertEquals(12_345L, parseTimestamp(" 00:12.345 "))
        assertEquals(5_000L, parseTimestamp("5."))
        assertEquals(90_000L, parseTimestamp("90"))
    }

    @Test
    fun fractionsAreTruncatedToMilliseconds() {
        assertEquals(1_234L, parseTimestamp("1.23456"))
        assertEquals(1_050L, parseTimestamp("1.05"))
    }

    @Test
    fun rejectsMalformedText() {
        listOf("", " ", ":", "1::2", "1:2:3:4", "-5", "+5", "5.-3", "1.2.3", ".5", "a", "1:xx", "5.1e").forEach { assertNull(it, parseTimestamp(it)) }
    }

    @Test
    fun overflowIsRejectedInsteadOfWrapping() {
        assertNull(parseTimestamp("9223372036854776"))
        assertNull(parseTimestamp("99999999999999999999"))
    }
}
