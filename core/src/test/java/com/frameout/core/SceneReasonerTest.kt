package com.frameout.core

import com.frameout.core.quality.NormBox
import com.frameout.core.quality.SceneReasoner
import com.frameout.core.quality.TagHit
import com.frameout.core.quality.TagRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneReasonerTest {
    private val face = NormBox(0.30f, 0.20f, 0.60f, 0.55f)
    private val faceHit = TagHit(TagRules.FACE, 1f, listOf(face))

    private fun tags(hits: List<TagHit>) = hits.map { it.tag }.toSet()

    @Test fun girlIsNotADog() {
        val out = SceneReasoner.refine(listOf(
            faceHit,
            TagHit("l:Dog", 0.88f), TagHit("l:Mammal", 0.80f), TagHit("l:Fur", 0.75f), TagHit("l:Smile", 0.9f)
        ))
        assertFalse("l:Dog" in tags(out)); assertFalse("l:Fur" in tags(out)); assertFalse("l:Mammal" in tags(out))
        assertTrue("l:Person" in tags(out)); assertTrue("l:Smile" in tags(out))
    }

    @Test fun animalBoxOnAFaceIsDroppedEvenWhenConfident() {
        val onFace = TagHit("l:Cat", 0.96f, listOf(NormBox(0.2f, 0.1f, 0.7f, 0.9f)))
        assertFalse("l:Cat" in tags(SceneReasoner.refine(listOf(faceHit, onFace))))
    }

    @Test fun realPetBesideAPersonSurvives() {
        val pet = TagHit("l:Dog", 0.95f, listOf(NormBox(0.70f, 0.60f, 0.95f, 0.95f)))
        assertTrue("l:Dog" in tags(SceneReasoner.refine(listOf(faceHit.copy(boxes = listOf(NormBox(0.1f, 0.1f, 0.2f, 0.2f))), pet))))
    }

    @Test fun dogWithoutPeopleKeepsSpecificAndHidesParents() {
        val out = SceneReasoner.refine(listOf(TagHit("l:Dog", 0.9f), TagHit("l:Mammal", 0.88f), TagHit("l:Pet", 0.8f), TagHit("l:Grass", 0.8f)))
        assertEquals(setOf("l:Dog", "l:Grass"), tags(out))
    }

    @Test fun lowConfidenceAndNoiseAreDropped() {
        val out = SceneReasoner.refine(listOf(TagHit("l:Tree", 0.65f), TagHit("l:Fun", 0.8f), TagHit("l:Sky", 0.9f)))
        assertEquals(setOf("l:Sky"), tags(out))
    }

    @Test fun exclusiveGendersKeepBestForSinglePerson() {
        val out = SceneReasoner.refine(listOf(faceHit, TagHit("l:Girl", 0.9f), TagHit("l:Man", 0.75f)))
        assertTrue("l:Girl" in tags(out)); assertFalse("l:Man" in tags(out))
    }

    @Test fun plantOnAFaceIsRejectedButClothesStay() {
        val plant = TagHit("o:plant", 0.7f, listOf(NormBox(0.3f, 0.2f, 0.6f, 0.5f)))
        val shirt = TagHit("o:fashion_good", 0.7f, listOf(NormBox(0.2f, 0.5f, 0.7f, 0.9f)))
        val out = tags(SceneReasoner.refine(listOf(faceHit, plant, shirt)))
        assertFalse("o:plant" in out); assertTrue("o:fashion_good" in out)
    }

    private fun frameWithPerson() = listOf(TagHit(TagRules.FACE, 1f, listOf(face)), TagHit("l:Person", 0.95f))

    @Test fun flickeringLabelIsDroppedWhenNeighboursDisagree() {
        val neighbours = List(4) { frameWithPerson() }
        val out = SceneReasoner.refine(listOf(TagHit("l:Bicycle", 0.75f), TagHit("l:Person", 0.95f)), neighbours)
        assertFalse("l:Bicycle" in tags(out)); assertTrue("l:Person" in tags(out))
    }

    @Test fun stableLabelSurvivesBelowTheNormalThreshold() {
        val neighbours = List(4) { listOf(TagHit("l:Beach", 0.8f)) }
        assertTrue("l:Beach" in tags(SceneReasoner.refine(listOf(TagHit("l:Beach", 0.63f)), neighbours)))
        assertFalse("l:Beach" in tags(SceneReasoner.refine(listOf(TagHit("l:Beach", 0.63f)))))
    }

    @Test fun personInNeighboursStillBlocksAnimalWhenFaceIsBlurred() {
        val neighbours = List(4) { frameWithPerson() }
        assertFalse("l:Dog" in tags(SceneReasoner.refine(listOf(TagHit("l:Dog", 0.85f)), neighbours)))
    }

    @Test fun tooFewNeighboursAreIgnored() {
        val neighbours = List(2) { frameWithPerson() }
        assertTrue("l:Dog" in tags(SceneReasoner.refine(listOf(TagHit("l:Dog", 0.85f)), neighbours)))
    }

    private val carBody = TagHit(TagRules.BODY, 0.8f, listOf(NormBox(0.05f, 0.2f, 0.95f, 0.8f)))
    private val carLabels = listOf(TagHit("l:Car", 0.9f), TagHit("l:Sports car", 0.85f), TagHit("l:Automotive design", 0.8f))
    private val carAsFashion = TagHit("o:fashion_good", 0.75f, listOf(NormBox(0.05f, 0.2f, 0.95f, 0.8f)))

    @Test fun skeletonOnACarIsNotABodyAndAddsNoPerson() {
        val out = tags(SceneReasoner.refine(carLabels + carBody))
        assertFalse(TagRules.BODY in out); assertFalse("l:Person" in out)
    }

    @Test fun fashionGuessOnACarIsDropped() {
        assertFalse("o:fashion_good" in tags(SceneReasoner.refine(carLabels + carBody + carAsFashion)))
        assertFalse("o:fashion_good" in tags(SceneReasoner.refine(carLabels + carAsFashion)))
    }

    @Test fun fashionWithoutAnyPersonIsDroppedEvenWithoutACar() {
        assertFalse("o:fashion_good" in tags(SceneReasoner.refine(listOf(TagHit("l:Wall", 0.9f), carAsFashion))))
    }

    @Test fun homeGoodGuessOnACarNeedsVeryHighConfidence() {
        val home = carAsFashion.copy(tag = "o:home_good")
        assertFalse("o:home_good" in tags(SceneReasoner.refine(carLabels + home)))
        assertTrue("o:home_good" in tags(SceneReasoner.refine(carLabels + home.copy(confidence = 0.95f))))
    }

    @Test fun bodyIsKeptWhenAFaceOrAPersonLabelAgrees() {
        assertTrue(TagRules.BODY in tags(SceneReasoner.refine(listOf(faceHit, carBody))))
        assertTrue(TagRules.BODY in tags(SceneReasoner.refine(listOf(TagHit("l:Person", 0.9f), carBody))))
    }

    @Test fun veryCompleteSkeletonSurvivesWhenNoCarIsAround() {
        assertTrue(TagRules.BODY in tags(SceneReasoner.refine(listOf(TagHit("l:Beach", 0.9f), carBody.copy(confidence = 0.9f)))))
        assertFalse(TagRules.BODY in tags(SceneReasoner.refine(listOf(TagHit("l:Beach", 0.9f), carBody.copy(confidence = 0.4f)))))
    }

    @Test fun personWithAShirtStillGetsFashion() {
        val shirt = TagHit("o:fashion_good", 0.7f, listOf(NormBox(0.2f, 0.5f, 0.7f, 0.9f)))
        assertTrue("o:fashion_good" in tags(SceneReasoner.refine(listOf(faceHit, shirt))))
    }

    @Test fun duplicateBoxesAreMerged() {
        val big = NormBox(0.1f, 0.1f, 0.5f, 0.5f)
        val near = NormBox(0.11f, 0.11f, 0.5f, 0.5f)
        assertEquals(1, SceneReasoner.dedupe(listOf(near, big)).size)
    }
}
