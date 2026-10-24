package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagRulesTest {
    private fun face(faces: Int, open: Float, smile: Float, level: FaceLevel) =
        FaceAssessment(faces, open, 0.8f, 0.8f, 0.9f, smile, 0.8f, level)

    @Test
    fun noFaceMeansNoTags() {
        assertTrue(TagRules.faceTags(FaceAssessment.NONE).isEmpty())
    }

    @Test
    fun smilingGoodFaceGetsAllPositiveTags() {
        val tags = TagRules.faceTags(face(1, 0.9f, 0.9f, FaceLevel.Good)).map { it.tag }
        assertTrue(tags.containsAll(listOf(TagRules.FACE, TagRules.SMILE, TagRules.GOOD_FACE)))
        assertTrue(TagRules.EYES_CLOSED !in tags)
    }

    @Test
    fun closedEyesAndGroupAreTagged() {
        val tags = TagRules.faceTags(face(4, 0.1f, 0f, FaceLevel.Poor)).map { it.tag }
        assertTrue(TagRules.EYES_CLOSED in tags)
        assertTrue(TagRules.GROUP in tags)
    }

    @Test
    fun poseNeedsAHumanShapedSkeleton() {
        assertTrue(TagRules.poseTags(PoseFixtures.person(only = setOf(0, 2, 5))).isEmpty())
        assertEquals(TagRules.BODY, TagRules.poseTags(PoseFixtures.person()).single().tag)
    }

    @Test
    fun confidentLandmarksSpreadOnACarGiveNoBody() {
        assertTrue(TagRules.poseTags(PoseFixtures.spreadOnACar()).isEmpty())
    }

    @Test
    fun shortOrBlankTextIsIgnored() {
        assertTrue(TagRules.textTags("  a b ").isEmpty())
        assertEquals(TagRules.TEXT, TagRules.textTags("Exit sign").single().tag)
    }

    @Test
    fun barcodeProducesGenericAndSpecificTags() {
        assertEquals(listOf(TagRules.CODE, "b:url"), TagRules.barcodeTags(8).map { it.tag })
        assertEquals(listOf(TagRules.CODE), TagRules.barcodeTags(0).map { it.tag })
    }

    @Test
    fun mergeKeepsHighestConfidence() {
        val merged = TagRules.merge(listOf(TagHit("l:Car", 0.7f), TagHit("l:Car", 0.9f), TagHit("l:Sky", 0.8f)))
        assertEquals(2, merged.size)
        assertEquals(0.9f, merged.first { it.tag == "l:Car" }.confidence, 0.0001f)
    }

    @Test
    fun objectTagsAreNormalised() {
        assertEquals("o:home_good", TagRules.objectTag("Home good", 0.8f).tag)
    }

    @Test
    fun namesAndGroups() {
        assertEquals(TagGroup.People, TagNames.group(TagRules.SMILE))
        assertEquals(TagGroup.Things, TagNames.group("l:Car"))
        assertEquals("سيارة", TagNames.display("l:Car", "ar"))
        assertEquals("Car", TagNames.display("l:Car", "en"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "ar"))
        assertEquals("وجوه", TagNames.display(TagRules.FACE, "ar"))
    }

    @Test
    fun faceTagsPointAtTheRightFaces() {
        val happy = FaceBox(NormBox(0.1f, 0.1f, 0.3f, 0.4f), smile = 0.9f, eyesOpen = 0.9f)
        val blink = FaceBox(NormBox(0.5f, 0.1f, 0.7f, 0.4f), smile = 0.1f, eyesOpen = 0.1f)
        val assessment = FaceAssessment(2, 0.1f, 0.8f, 0.8f, 0.9f, 0.5f, 0.8f, FaceLevel.Fair, listOf(happy, blink))
        val hits = TagRules.faceTags(assessment).associateBy { it.tag }
        assertEquals(listOf(happy.box, blink.box), hits.getValue(TagRules.FACE).boxes)
        assertEquals(listOf(blink.box), hits.getValue(TagRules.EYES_CLOSED).boxes)
    }

    @Test
    fun mergeCombinesBoxesOfTheSameTag() {
        val a = NormBox(0.1f, 0.1f, 0.3f, 0.3f)
        val b = NormBox(0.5f, 0.5f, 0.9f, 0.9f)
        val merged = TagRules.merge(listOf(TagHit("l:Car", 0.7f), TagHit("l:Car", 0.9f, listOf(a)), TagHit("l:Car", 0.8f, listOf(a, b))))
        assertEquals(listOf(a, b), merged.single().boxes)
    }

    @Test
    fun everyDefaultModelLabelHasArabicName() {
        assertEquals(430, TagLabelsAr.MAP.size)
        assertTrue(TagLabelsAr.MAP.all { (en, ar) -> ar.isNotBlank() && ar != en })
        assertEquals("مسجد", TagNames.display("l:Mosque", "ar"))
        assertEquals("بابا نويل", TagNames.display("l:Santa Claus", "ar"))
        assertEquals("أدوات الطبخ والخَبز", TagNames.display("l:Cookware and bakeware", "ar"))
        assertEquals("Mosque", TagNames.display("l:Mosque", "en"))
    }
}
