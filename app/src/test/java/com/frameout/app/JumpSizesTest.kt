package com.frameout.app

import com.frameout.app.trim.JumpSizes
import org.junit.Assert.assertEquals
import org.junit.Test

class JumpSizesTest {
    @Test
    fun cyclesThroughOptionsAndWraps() {
        assertEquals(10, JumpSizes.next(5))
        assertEquals(60, JumpSizes.next(30))
        assertEquals(5, JumpSizes.next(60))
    }

    @Test
    fun unknownValueMovesToNextHigherOrWraps() {
        assertEquals(15, JumpSizes.next(11))
        assertEquals(5, JumpSizes.next(1000))
    }

    @Test
    fun normalizeFallsBackToDefault() {
        assertEquals(15, JumpSizes.normalize(15))
        assertEquals(JumpSizes.DEFAULT, JumpSizes.normalize(7))
    }
}
