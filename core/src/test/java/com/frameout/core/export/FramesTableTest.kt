package com.frameout.core.export

import org.junit.Assert.assertEquals
import org.junit.Test

class FramesTableTest {
    private fun row(sequence: Int, source: String = "clip.mp4") = FrameRow(sequence, sequence * 10, 100L + sequence, 1_500_000L * sequence, sequence == 1, sequence == 2, source)

    @Test
    fun headerAndRowsUseCrlfAndBom() {
        val lines = FramesTable.csv(listOf(row(1), row(2))).removePrefix(CsvText.BOM).split(CsvText.EOL)
        assertEquals(FramesTable.HEADER, lines[0])
        assertEquals("1,10,101,1500,00:01.500,1,0,clip.mp4", lines[1])
        assertEquals("2,20,102,3000,00:03.000,0,1,clip.mp4", lines[2])
        assertEquals("", lines[3])
    }

    @Test
    fun emptyListHasOnlyHeader() = assertEquals(FramesTable.HEADER + CsvText.EOL, FramesTable.csv(emptyList()).removePrefix(CsvText.BOM))

    @Test
    fun quotesCommasAndQuotes() {
        assertEquals("\"a,b\"", CsvText.quote("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvText.quote("say \"hi\""))
        assertEquals("plain", CsvText.quote("plain"))
    }
}
