package com.frameout.app.frames

import com.frameout.core.db.FrameEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameReassemblerTest {
    @Test
    fun separatedIndexesNeverMergeAtHighFrameRate() {
        val frames = listOf(frame(0, 0), frame(1, 16_667), frame(3, 50_001), frame(4, 66_668))
        val ranges = FrameReassembler.buildRanges(frames)
        assertEquals(2, ranges.size)
        assertEquals(0L, ranges[0].startUs)
        assertEquals(33_334L, ranges[0].endUs)
        assertEquals(50_001L, ranges[1].startUs)
    }

    private fun frame(index: Int, pts: Long) = FrameEntity(projectId = 1L, index = index, ptsUs = pts)
}
