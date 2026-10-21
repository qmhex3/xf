package com.frameout.core.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class ProShowcaseTest {
    @Test
    fun everyShowcaseKindHasExactlyOneFeature() {
        val mapped = ProFeature.entries.mapNotNull { ProShowcase.kindOf(it) }
        assertEquals(ShowcaseKind.entries.toSet(), mapped.toSet())
        assertEquals(ShowcaseKind.entries.size, mapped.size)
    }

    @Test
    fun generalAndLongTailFeaturesHaveNoShowcase() {
        listOf(ProFeature.General, ProFeature.Collage, ProFeature.Batch, ProFeature.Compare, ProFeature.AppIcons, ProFeature.Insights)
            .forEach { assertNull(it.key, ProShowcase.kindOf(it)) }
    }

    @Test
    fun parseSplitsTrimsAndDropsEmpty() {
        assertEquals(listOf("A", "B", "C"), ProShowcase.parse(" A | B||C |"))
    }

    @Test
    fun onlyShapesHideExtraItems() {
        assertEquals(14, ProShowcase.hiddenCount(ShowcaseKind.Shapes))
        ShowcaseKind.entries.filter { it != ShowcaseKind.Shapes }.forEach { assertEquals(0, ProShowcase.hiddenCount(it)) }
    }

    @Test
    fun proFilterListExcludesFreeFilters() {
        val ids = ProShowcase.proFilterIds()
        assertTrue(ids.isNotEmpty())
        ids.forEach { assertFalse(it, ProCatalog.filterIsFree(it)) }
        assertFalse(ids.contains(""))
    }
}
