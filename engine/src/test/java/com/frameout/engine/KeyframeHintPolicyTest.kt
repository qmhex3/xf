package com.frameout.engine

import com.frameout.engine.decode.KeyframeHintPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyframeHintPolicyTest {
    @Test
    fun sequentialAccessNeedsNoHint() {
        assertFalse(KeyframeHintPolicy.needed(10, 11))
        assertFalse(KeyframeHintPolicy.needed(10, 18))
    }

    @Test
    fun largeForwardGapNeedsHint() {
        assertTrue(KeyframeHintPolicy.needed(10, 19))
        assertTrue(KeyframeHintPolicy.needed(0, 5000))
    }

    @Test
    fun backwardJumpNeedsHint() {
        assertTrue(KeyframeHintPolicy.needed(100, 100))
        assertTrue(KeyframeHintPolicy.needed(100, 40))
    }

    @Test
    fun freshSessionOnlyHintsWhenFarFromStart() {
        assertFalse(KeyframeHintPolicy.needed(-1, 3))
        assertTrue(KeyframeHintPolicy.needed(-1, 500))
    }
}
