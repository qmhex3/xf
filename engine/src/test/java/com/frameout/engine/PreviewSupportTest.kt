package com.frameout.engine

import com.frameout.engine.player.PreviewSupport
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewSupportTest {
    @Test
    fun realMediaAndMxfAreUnplayable() {
        assertTrue(PreviewSupport.exoCannotPlay("realmedia", "bin"))
        assertTrue(PreviewSupport.exoCannotPlay("mxf", "mp4"))
    }

    @Test
    fun extensionDecidesOnlyWhenTheHeaderIsUnknown() {
        assertTrue(PreviewSupport.exoCannotPlay(null, "RMVB"))
        assertTrue(PreviewSupport.exoCannotPlay(null, "mxf"))
        assertFalse(PreviewSupport.exoCannotPlay("mp4", "rm"))
        assertFalse(PreviewSupport.exoCannotPlay(null, "mp4"))
    }
}
