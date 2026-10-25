package com.frameout.core.quality

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockLedgerTest {
    private fun temp(): File = File.createTempFile("ledger", "").let { it.delete(); it.mkdirs(); it }

    @Test
    fun roundTripsFactorsAndKeepsTheLatestEntry() {
        val dir = temp()
        BlockLedger.record(dir, 3, 0.4)
        BlockLedger.record(dir, 5, 0.875)
        BlockLedger.record(dir, 3, 0.25)
        val read = BlockLedger.read(dir)
        assertEquals(0.25, read.getValue(3), 0.001)
        assertEquals(0.875, read.getValue(5), 0.001)
    }

    @Test
    fun neutralFramesWriteNothing() {
        val dir = temp()
        BlockLedger.record(dir, 1, 1.0)
        assertTrue(BlockLedger.read(dir).isEmpty())
        assertTrue(!BlockLedger.fileIn(dir).exists())
    }

    @Test
    fun damagedLinesAreIgnored() {
        val dir = temp()
        BlockLedger.record(dir, 2, 0.5)
        BlockLedger.fileIn(dir).appendText("garbage\n7\tx\n9\t")
        assertEquals(setOf(2), BlockLedger.read(dir).keys)
    }

    @Test
    fun projectFolderIsDerivedFromTheThumbnailPath() {
        assertEquals(File("/data/projects/9"), BlockLedger.projectDirOf("/data/projects/9/thumbs/t_4.jpg"))
        assertNull(BlockLedger.projectDirOf("/data/projects/9/other/t_4.jpg"))
        assertNull(BlockLedger.projectDirOf(null))
    }

    @Test
    fun lookupDefaultsToOneAndReadsTheRightProject() {
        val dir = temp()
        BlockLedger.record(dir, 4, 0.5)
        val lookup = BlockLedger.Lookup()
        val thumb = File(File(dir, "thumbs"), "t_4.jpg").path
        assertEquals(0.5, lookup.factor(thumb, 4), 0.001)
        assertEquals(1.0, lookup.factor(thumb, 8), 0.0)
        assertEquals(1.0, lookup.factor(null, 4), 0.0)
    }
}
