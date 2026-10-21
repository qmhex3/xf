package com.frameout.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSheetLayoutTest {
    private val options = ContactSheetLayout.Options()

    @Test
    fun columnsAreClamped() {
        assertEquals(2, ContactSheetLayout.Options(columns = 0).safeColumns)
        assertEquals(8, ContactSheetLayout.Options(columns = 99).safeColumns)
    }

    @Test
    fun smallSetFitsOneSheetAndCellsStayInsideTheWidth() {
        val plan = ContactSheetLayout.plan(10, 16f / 9f, options)
        assertEquals(1, plan.sheets.size)
        assertEquals(0 until 10, plan.sheets[0])
        val lastLeft = plan.cellLeft(plan.columns - 1)
        assertTrue(lastLeft + plan.cellW <= plan.sheetWidth - ContactSheetLayout.MARGIN)
    }

    @Test
    fun everyFrameLandsOnExactlyOneSheet() {
        val plan = ContactSheetLayout.plan(437, 9f / 16f, ContactSheetLayout.Options(columns = 3))
        assertEquals(437, plan.sheets.sumOf { it.count() })
        assertEquals(0, plan.sheets.first().first)
        plan.sheets.zipWithNext().forEach { (a, b) -> assertEquals(a.last + 1, b.first) }
        assertEquals(436, plan.sheets.last().last)
    }

    @Test
    fun sheetsNeverExceedThePixelBudget() {
        val plan = ContactSheetLayout.plan(300, 9f / 16f, ContactSheetLayout.Options(columns = 2))
        plan.sheets.indices.forEach { i ->
            assertTrue(plan.sheetWidth.toLong() * plan.sheetHeight(i) <= ContactSheetLayout.MAX_SHEET_PIXELS)
        }
    }

    @Test
    fun captionsAddHeightOnlyWhenWanted() {
        val with = ContactSheetLayout.plan(8, 1.5f, options)
        val without = ContactSheetLayout.plan(8, 1.5f, options.copy(showTime = false, showNumber = false))
        assertTrue(with.captionH > 0)
        assertEquals(0, without.captionH)
        assertTrue(with.sheetHeight(0) > without.sheetHeight(0))
    }

    @Test
    fun hugeSelectionsAreFlaggedInsteadOfCreatingEndlessFiles() {
        assertTrue(ContactSheetLayout.plan(20_000, 9f / 16f, options).tooMany)
        assertFalse(ContactSheetLayout.plan(40, 16f / 9f, options).tooMany)
    }

    @Test
    fun badAspectFallsBackToWidescreen() {
        val plan = ContactSheetLayout.plan(4, Float.NaN, options)
        assertEquals((plan.cellW / (16f / 9f)).toInt(), plan.cellH)
        assertTrue(ContactSheetLayout.plan(0, 1f, options).sheets.isEmpty())
    }

    @Test
    fun fitKeepsAspectAndCentres() {
        val box = ContactSheetLayout.fit(100, 200, 200, 200)
        assertEquals(50f, box[0], 0.01f)
        assertEquals(0f, box[1], 0.01f)
        assertEquals(150f, box[2], 0.01f)
        assertEquals(200f, box[3], 0.01f)
    }

    @Test
    fun fileNamesAreNumberedOnlyWhenSplit() {
        assertEquals("A_contact_5.jpg", ContactSheetLayout.fileName("A", 5, 0, 1, false))
        assertEquals("A_contact_5_2of3.png", ContactSheetLayout.fileName("A", 5, 1, 3, true))
    }
}
