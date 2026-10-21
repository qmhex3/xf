package com.frameout.engine

import com.frameout.engine.index.IndexReuse
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexReuseTest {
    @Test
    fun reusesWhenEverythingMatches() = assertTrue(IndexReuse.canReuse(900, 900, 30_000L, 30_000_400L))

    @Test
    fun rejectsMissingMarkerOrEmptyIndex() {
        assertFalse(IndexReuse.canReuse(null, 900, 30_000L, 30_000_000L))
        assertFalse(IndexReuse.canReuse(0, 0, 30_000L, 30_000_000L))
    }

    @Test
    fun rejectsCountMismatch() = assertFalse(IndexReuse.canReuse(900, 899, 30_000L, 30_000_000L))

    @Test
    fun rejectsReplacedSource() {
        assertFalse(IndexReuse.canReuse(900, 900, 30_000L, 45_000_000L))
        assertFalse(IndexReuse.canReuse(900, 900, 0L, 30_000_000L))
        assertFalse(IndexReuse.canReuse(900, 900, 30_000L, 0L))
    }

    @Test
    fun markerRoundTripsAndClears() {
        val dir = File.createTempFile("idx", "d").apply { delete(); mkdirs(); deleteOnExit() }
        assertNull(IndexReuse.readMarker(dir))
        IndexReuse.writeMarker(dir, 1234)
        assertEquals(1234, IndexReuse.readMarker(dir))
        IndexReuse.clearMarker(dir)
        assertNull(IndexReuse.readMarker(dir))
    }

    @Test
    fun corruptMarkerIsIgnored() {
        val dir = File.createTempFile("idx", "d").apply { delete(); mkdirs(); deleteOnExit() }
        File(dir, "index.system").writeText("not a number")
        assertNull(IndexReuse.readMarker(dir))
    }
}
