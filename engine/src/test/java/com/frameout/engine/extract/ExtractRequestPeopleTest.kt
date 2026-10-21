package com.frameout.engine.extract

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtractRequestPeopleTest {
    @Test
    fun bodiesAreIncludedByDefault() =
        assertEquals(true, ExtractRequest(1L, "content://v", 0L, 1000L, ExtractMode.EveryFrame, false).peopleBody)
}
