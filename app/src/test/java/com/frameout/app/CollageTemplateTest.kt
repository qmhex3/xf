package com.frameout.app

import com.frameout.core.collage.CollageTemplates
import com.frameout.core.collage.toPx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollageTemplateTest {
    @Test
    fun suggestPicksSmallestTemplateThatFits() {
        assertEquals("2x1", CollageTemplates.suggest(0).id)
        assertEquals("2x1", CollageTemplates.suggest(2).id)
        assertEquals("2x2", CollageTemplates.suggest(4).id)
        assertEquals("film_strip", CollageTemplates.suggest(5).id)
        assertEquals("2x3", CollageTemplates.suggest(6).id)
        assertEquals("contact_sheet", CollageTemplates.suggest(99).id)
    }

    @Test
    fun templateIdsAreUnique() {
        val ids = CollageTemplates.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun outerMarginEqualsGapAndInnerGapIsFullGap() {
        val grid = CollageTemplates.all.first { it.id == "2x2" }
        val a = grid.cells[0].toPx(1000f, 1000f, 20f)
        val b = grid.cells[1].toPx(1000f, 1000f, 20f)
        assertEquals(20f, a.left, 0.01f)
        assertEquals(20f, a.top, 0.01f)
        assertEquals(20f, b.left - a.right, 0.01f)
        assertEquals(980f, b.right, 0.01f)
    }

    @Test
    fun cellsNeverCollapseEvenWithHugeGap() {
        CollageTemplates.all.forEach { t ->
            t.cells.forEach { c -> assertTrue(c.toPx(100f, 100f, 400f).width >= 1f) }
        }
    }
}
