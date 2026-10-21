package com.frameout.engine

import com.frameout.codecpack.FormatKind
import com.frameout.engine.decode.AnimatedSources
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedSourcesTest {
    @Test
    fun animatedKindsAreHandledWithoutTheCodecPack() {
        assertTrue(AnimatedSources.handles(FormatKind.Gif))
        assertTrue(AnimatedSources.handles(FormatKind.Apng))
        assertTrue(AnimatedSources.handles(FormatKind.AnimatedWebp))
    }

    @Test
    fun codecPackAndSystemKindsAreNotHandled() {
        assertFalse(AnimatedSources.handles(FormatKind.NeedsFfmpeg))
        assertFalse(AnimatedSources.handles(FormatKind.System))
        assertFalse(AnimatedSources.handles(FormatKind.Unsupported))
    }
}
