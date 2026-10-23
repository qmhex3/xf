package com.frameout.engine.extract

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExtractRequestGateTest {
    private fun request(mode: ExtractMode = ExtractMode.EveryFrame, blurry: Boolean = false, dup: Boolean = false, people: Boolean = false) =
        ExtractRequest(1L, "content://v", 0L, 1000L, mode, false, skipBlurry = blurry, skipDuplicates = dup, peopleOnly = people)

    @Test
    fun noFiltersMeansNoGate() = assertNull(request().newGate())

    @Test
    fun peopleOnlyCreatesGate() = assertNotNull(request(people = true).newGate())

    @Test
    fun filtersApplyToFixedCountToo() = assertNotNull(request(ExtractMode.FixedCount(10), blurry = true).newGate())

    private val vehicles = ExtractMode.Content(com.frameout.core.quality.content.ContentSpec(tags = setOf("car")))

    @Test
    fun peopleOnlyIsDroppedWhenContentTagsDecideWhatToKeep() {
        val r = request(vehicles, people = true)
        org.junit.Assert.assertFalse(r.peopleOnlyApplied)
        assertNotNull(r.newGate())
    }

    @Test
    fun peopleOnlyIsDroppedForSlides() =
        org.junit.Assert.assertFalse(request(ExtractMode.Slides, people = true).peopleOnlyApplied)

    @Test
    fun peopleOnlyStillWorksWhenNoTagIsPickedOrModeIsPlain() {
        org.junit.Assert.assertTrue(request(ExtractMode.Content(com.frameout.core.quality.content.ContentSpec()), people = true).peopleOnlyApplied)
        org.junit.Assert.assertTrue(request(ExtractMode.EveryFrame, people = true).peopleOnlyApplied)
        org.junit.Assert.assertTrue(request(ExtractMode.SceneChange, people = true).peopleOnlyApplied)
    }
}
