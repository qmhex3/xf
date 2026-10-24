package com.frameout.core.quality.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentSpecTest {
    @Test
    fun savedSpecWithOldStepAndGapStillReadsStrictnessAndMatch() {
        val spec = ContentSpec.decode("2000:2:5000:1:")
        assertEquals(ContentSpec.STRICT_STRICT, spec.strict)
        assertTrue(spec.all)
    }

    @Test
    fun encodedSpecRoundTripsWithoutStepOrGap() {
        val spec = ContentSpec(strict = ContentSpec.STRICT_LOOSE, all = true)
        assertEquals(spec, ContentSpec.decode(spec.encode()))
        assertTrue(spec.encode().startsWith("1:0:0:1:"))
    }

    @Test
    fun brokenTextFallsBackToDefaults() {
        assertEquals(ContentSpec(), ContentSpec.decode("nonsense"))
    }
}
