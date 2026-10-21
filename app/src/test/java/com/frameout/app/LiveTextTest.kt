package com.frameout.app

import androidx.compose.ui.geometry.Offset
import com.frameout.app.viewer.LiveTextGeometry
import com.frameout.app.viewer.LiveTextState
import com.frameout.core.quality.NormBox
import com.frameout.core.quality.ScannedWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTextTest {
    private val words = listOf(
        ScannedWord("Hello", NormBox(0.10f, 0.10f, 0.30f, 0.15f), 0),
        ScannedWord("world", NormBox(0.35f, 0.10f, 0.55f, 0.15f), 0),
        ScannedWord("Second", NormBox(0.10f, 0.30f, 0.40f, 0.35f), 1)
    )

    @Test fun selectionJoinsWordsAndLines() {
        val s = LiveTextState(active = true, words = words, anchor = 0, focus = 2)
        assertEquals("Hello world\nSecond", s.selectedText())
    }

    @Test fun backwardsRangeIsNormalised() {
        val s = LiveTextState(active = true, words = words, anchor = 2, focus = 1)
        assertEquals("world\nSecond", s.selectedText())
        assertTrue(s.isSelected(1) && s.isSelected(2) && !s.isSelected(0))
    }

    @Test fun noSelectionIsEmpty() {
        assertEquals("", LiveTextState(active = true, words = words).selectedText())
    }

    @Test fun hitTestAtIdentityTransform() {
        val hit = LiveTextGeometry.nearest(words, Offset(200f, 125f), 1000f, 1000f, 1000, 1000, 1f, 0f, 0f, 10f)
        assertEquals(0, hit)
        val miss = LiveTextGeometry.nearest(words, Offset(800f, 800f), 1000f, 1000f, 1000, 1000, 1f, 0f, 0f, 10f)
        assertEquals(-1, miss)
    }

    @Test fun hitTestFollowsZoomAndPan() {
        val view = Offset(500f + (200f - 500f) * 2f + 100f, 500f + (125f - 500f) * 2f)
        assertEquals(0, LiveTextGeometry.nearest(words, view, 1000f, 1000f, 1000, 1000, 2f, 100f, 0f, 10f))
    }
}
