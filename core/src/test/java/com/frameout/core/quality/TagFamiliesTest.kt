package com.frameout.core.quality

import com.frameout.core.db.FrameEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagFamiliesTest {
    private val a = NormBox(0.1f, 0.1f, 0.3f, 0.3f)
    private val b = NormBox(0.5f, 0.5f, 0.9f, 0.9f)

    private fun frame(id: Long, index: Int) = FrameEntity(id = id, projectId = 1, index = index, ptsUs = index * 1000L)

    @Test
    fun differentVehiclesCollapseIntoOneFamilyWithBestConfidenceAndAllBoxes() {
        val out = TagFamilies.withFamilies(
            listOf(TagHit("l:Car", 0.9f, listOf(a)), TagHit("l:Airplane", 0.8f, listOf(b)), TagHit("l:Bicycle", 0.7f))
        )
        val family = out.filter { it.tag == TagFamilies.key("vehicles") }
        assertEquals(1, family.size)
        assertEquals(0.9f, family.single().confidence, 0.0001f)
        assertEquals(listOf(a, b), family.single().boxes)
        assertEquals(3, out.count { it.tag.startsWith(TagRules.LABEL) })
    }

    @Test
    fun matchingIgnoresCaseAndSurroundingSpaces() {
        assertEquals("vehicles", TagFamilies.familyOfLabel("  airplane "))
        assertEquals("food", TagFamilies.familyOfLabel("Fast food"))
        assertEquals("food", TagFamilies.familyOfLabel("hot dog"))
    }

    @Test
    fun unknownLabelFallsIntoOther() {
        assertEquals(TagFamilies.OTHER, TagFamilies.familyOfLabel("Zorblax"))
        assertEquals(TagFamilies.OTHER, TagFamilies.familyOfLabel("Rectangle"))
        val out = TagFamilies.withFamilies(listOf(TagHit("l:Zorblax", 0.8f)))
        assertTrue(out.any { it.tag == TagFamilies.key(TagFamilies.OTHER) })
    }

    @Test
    fun objectCategoriesJoinTheSameFamiliesAsLabels() {
        val out = TagFamilies.withFamilies(listOf(TagHit("o:plant", 0.8f, listOf(a)), TagHit("l:Flower", 0.9f, listOf(b))))
        val plants = out.single { it.tag == TagFamilies.key("plants") }
        assertEquals(0.9f, plants.confidence, 0.0001f)
        assertEquals(listOf(a, b), plants.boxes)
    }

    @Test
    fun peopleAndTextTagsAreNotGrouped() {
        val hits = listOf(TagHit(TagRules.FACE, 1f), TagHit(TagRules.TEXT, 1f), TagHit(TagRules.CODE, 1f))
        assertEquals(hits, TagFamilies.withFamilies(hits))
    }

    @Test
    fun applyingTwiceDoesNotDuplicateFamilies() {
        val once = TagFamilies.withFamilies(listOf(TagHit("l:Dog", 0.9f), TagHit("l:Cat", 0.8f)))
        val twice = TagFamilies.withFamilies(once)
        assertEquals(once.map { it.tag }.sorted(), twice.map { it.tag }.sorted())
        assertEquals(1, twice.count { it.tag == TagFamilies.key("animals") })
    }

    @Test
    fun rawDetailsAreHiddenWhileFamiliesAndOtherKindsStayVisible() {
        assertFalse(TagFamilies.visible("l:Car"))
        assertFalse(TagFamilies.visible("o:food"))
        assertTrue(TagFamilies.visible(TagFamilies.key("vehicles")))
        assertTrue(TagFamilies.visible(TagRules.FACE))
        assertTrue(TagFamilies.visible(TagRules.TEXT))
        assertTrue(TagFamilies.visible("b:url"))
    }

    @Test
    fun everyFamilyHasTitlesAndNoLabelBelongsToTwoFamilies() {
        val seen = HashMap<String, String>()
        TagFamilies.all.forEach { family ->
            assertTrue(family.en.isNotBlank() && family.ar.isNotBlank())
            assertEquals(family.en, TagNames.display(family.key, false))
            assertEquals(family.ar, TagNames.display(family.key, true))
            family.labels.split('|').filter { it.isNotEmpty() }.forEach { label ->
                val previous = seen.put(label.lowercase(), family.id)
                assertTrue("$label in ${family.id} and $previous", previous == null)
            }
        }
        assertEquals(TagFamilies.all.size, TagFamilies.all.map { it.id }.toSet().size)
    }

    @Test
    fun stackPrefersFramesFarApartFromTheTopOne() {
        val top = frame(1, 0)
        val pool = listOf(frame(2, 1), frame(3, 2), frame(4, 40), frame(5, 80), frame(1, 0))
        val behind = CoverStack.behind(pool, top)
        assertEquals(2, behind.size)
        assertTrue(behind.none { it.id == top.id })
        assertTrue(behind.all { it.index >= 20 })
    }

    @Test
    fun stackFallsBackToWhatExists() {
        val top = frame(1, 0)
        assertTrue(CoverStack.behind(listOf(top), top).isEmpty())
        assertEquals(listOf(2L), CoverStack.behind(listOf(top, frame(2, 1)), top).map { it.id })
        assertEquals(2, CoverStack.behind(listOf(frame(2, 1), frame(3, 2), frame(4, 3)), top).size)
    }
}
