package com.frameout.core.quality

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseEvidenceTest {
    @Test
    fun aRealStandingSkeletonIsHumanShaped() {
        assertTrue(PoseEvidence.humanShaped(PoseFixtures.person()))
        assertTrue(PoseEvidence.humanShaped(PoseFixtures.person(), PoseEvidence.STRONG_CONFIDENT))
    }

    @Test
    fun confidentPointsOnACarAreNotABody() {
        assertFalse(PoseEvidence.humanShaped(PoseFixtures.spreadOnACar()))
    }

    @Test
    fun lowLikelihoodPointsDoNotCount() {
        assertFalse(PoseEvidence.humanShaped(PoseFixtures.person(likelihood = 0.7f)))
    }

    @Test
    fun tooFewPointsOrNoTorsoIsRejected() {
        assertFalse(PoseEvidence.humanShaped(PoseFixtures.person(only = setOf(0, 2, 5, 7, 8))))
        assertFalse(PoseEvidence.humanShaped(PoseFixtures.person(only = (0..10).toSet() + setOf(13, 14, 15, 16))))
    }

    @Test
    fun strictModeNeedsMoreLandmarks() {
        val upper = PoseFixtures.person(only = (0..16).toSet() + setOf(23, 24))
        assertTrue(PoseEvidence.humanShaped(upper))
        assertFalse(PoseEvidence.humanShaped(upper, PoseEvidence.STRONG_CONFIDENT))
    }
}
