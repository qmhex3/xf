package com.frameout.core.quality

import com.frameout.core.storage.ThumbSpec
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisSourceTest {
    private val thumb = File("/data/projects/7/thumbs/t_12.jpg").path
    private val full = File("/data/projects/7/frames/f_12.jpg").path
    private val copy = File("/data/projects/7/analysis/a_12.jpg").path

    @Test
    fun fullPathComesFirstThenAnalysisCopyThenThumbnail() {
        assertEquals(listOf(full, copy, thumb), AnalysisSource.candidates(full, thumb, 12))
    }

    @Test
    fun withoutFullPathTheAnalysisCopyBeatsTheThumbnail() {
        assertEquals(listOf(copy, thumb), AnalysisSource.candidates(null, thumb, 12))
    }

    @Test
    fun thumbnailOutsideThumbsFolderHasNoCopy() {
        val odd = File("/cache/t_12.jpg").path
        assertNull(AnalysisSource.copyPath(odd, 12))
        assertEquals(listOf(odd), AnalysisSource.candidates(null, odd, 12))
    }

    @Test
    fun blankAndDuplicatePathsAreDropped() {
        assertEquals(listOf(copy, thumb), AnalysisSource.candidates("", thumb, 12))
        assertEquals(1, AnalysisSource.candidates(thumb, thumb, 12).count { it == thumb })
        assertTrue(AnalysisSource.candidates(null, null, 1).isEmpty())
    }

    @Test
    fun copyIsOnlyNeededWhenTheSourceExceedsTheThumbnailSide() {
        assertFalse(AnalysisSource.needsCopy(ThumbSpec.MAX_SIDE, 300))
        assertTrue(AnalysisSource.needsCopy(ThumbSpec.MAX_SIDE + 1, 300))
        assertTrue(AnalysisSource.needsCopy(300, 1920))
    }

    @Test
    fun reducedSideIsSmallerThanTheNormalSide() {
        assertEquals(ThumbSpec.ANALYSIS_SIDE, AnalysisSource.side(false))
        assertEquals(ThumbSpec.ANALYSIS_SIDE_REDUCED, AnalysisSource.side(true))
        assertTrue(AnalysisSource.side(true) < AnalysisSource.side(false))
    }
}
