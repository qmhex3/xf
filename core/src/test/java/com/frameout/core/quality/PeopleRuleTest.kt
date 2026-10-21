package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleRuleTest {
    @Test
    fun aFaceIsEnough() = assertEquals(true, PeopleRule.isPerson(1, 0))

    @Test
    fun bodyNeedsEnoughConfidentLandmarks() {
        assertEquals(false, PeopleRule.isPerson(0, TagRules.MIN_LANDMARKS - 1))
        assertEquals(true, PeopleRule.isPerson(0, TagRules.MIN_LANDMARKS))
    }

    @Test
    fun emptySceneIsNotAPerson() = assertEquals(false, PeopleRule.isPerson(0, 0))
}
