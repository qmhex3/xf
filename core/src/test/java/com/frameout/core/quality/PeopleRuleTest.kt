package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleRuleTest {
    @Test
    fun aFaceIsEnough() = assertEquals(true, PeopleRule.isPerson(1, false))

    @Test
    fun aHumanShapedPoseIsEnoughWithoutAFace() = assertEquals(true, PeopleRule.isPerson(0, true))

    @Test
    fun emptySceneIsNotAPerson() = assertEquals(false, PeopleRule.isPerson(0, false))
}
