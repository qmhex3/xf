package com.frameout.codecpack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CodecPackSerialTest {
    @Test
    fun serialIsReadFromTheSignedManifestLine() {
        val payload = "Frame out codec pack\r\nFFmpeg 7.1\r\nSerial: 20261006120000\r\nABIs: arm64-v8a\r\n"
        assertEquals(20261006120000L, CodecPackSerial.parse(payload))
    }

    @Test
    fun missingOrMalformedSerialIsZero() {
        assertEquals(0L, CodecPackSerial.parse("Frame out codec pack\nFFmpeg 7.1\n"))
        assertEquals(0L, CodecPackSerial.parse("Serial: abc\n"))
        assertEquals(0L, CodecPackSerial.parse("x Serial: 5\n"))
    }

    @Test
    fun olderThanInstalledIsRejected() {
        assertNotNull(CodecPackSerial.rejection(5L, 9L, 0L))
        assertNull(CodecPackSerial.rejection(9L, 9L, 0L))
        assertNull(CodecPackSerial.rejection(10L, 9L, 0L))
    }

    @Test
    fun belowTheRevocationFloorIsRejectedEvenWhenNothingIsInstalled() {
        assertNotNull(CodecPackSerial.rejection(99L, 0L, 100L))
        assertNull(CodecPackSerial.rejection(100L, 0L, 100L))
    }

    @Test
    fun revokedSerialIsRejectedEvenWhenNewer() {
        assertNotNull(CodecPackSerial.rejection(50L, 10L, 1L, setOf(50L)))
        assertNull(CodecPackSerial.rejection(51L, 10L, 1L, setOf(50L)))
    }

    @Test
    fun packWithoutSerialIsRejectedByDefaultFloor() {
        assertNotNull(CodecPackSerial.rejection(CodecPackSerial.parse("no serial"), 0L, 1L))
    }
}
