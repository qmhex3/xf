package com.frameout.core.edit

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorMathTest {
    @Test
    fun primariesRoundTrip() {
        listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF34C759.toInt(), 0xFF0A84FF.toInt())
            .forEach { assertEquals(it, ColorMath.toArgb(ColorMath.fromArgb(it))) }
    }

    @Test
    fun hueSixtyIsYellow() {
        assertEquals(0xFFFFFF00.toInt(), ColorMath.toArgb(Hsv(60f, 1f, 1f)))
    }

    @Test
    fun hueWrapsAround() {
        assertEquals(ColorMath.toArgb(Hsv(10f, 1f, 1f)), ColorMath.toArgb(Hsv(370f, 1f, 1f)))
        assertEquals(0xFFFF0000.toInt(), ColorMath.toArgb(Hsv(360f, 1f, 1f)))
    }

    @Test
    fun hexIsUppercaseSixDigits() {
        assertEquals("#FF3B30", ColorMath.hex(0xFFFF3B30.toInt()))
        assertEquals("#000000", ColorMath.hex(0xFF000000.toInt()))
    }

    @Test
    fun grayHasNoSaturation() {
        val hsv = ColorMath.fromArgb(0xFF808080.toInt())
        assertEquals(0f, hsv.s, 0f)
        assertEquals(128f / 255f, hsv.v, 0.001f)
    }
}
