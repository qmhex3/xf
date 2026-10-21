package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Test

class SkipStatsTest {
    @Test
    fun countsAreTalliedPerReason() {
        val c = SkipCounts() + SkipReason.Blurry + SkipReason.Blurry + SkipReason.NoPeople
        assertEquals(SkipCounts(blurry = 2, noPeople = 1), c)
        assertEquals(3, c.total)
    }

    @Test
    fun gateReportsEachRejection() {
        val seen = ArrayList<SkipReason>()
        val size = 64
        val sharp = { p: Int -> IntArray(size * size) { i -> if (((i % size) / 2 + (i / size) / 2 + p) % 2 == 0) 20 else 230 } }
        val gate = ExtractQualityGate(true, true, false) { seen += it }
        assertEquals(true, gate.accept(sharp(0), size, size))
        assertEquals(false, gate.accept(sharp(0), size, size))
        assertEquals(listOf(SkipReason.Duplicate), seen)
    }
}
