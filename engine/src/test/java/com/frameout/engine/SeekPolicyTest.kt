package com.frameout.engine

import com.frameout.engine.extract.SeekPolicy
import com.frameout.engine.index.SampleInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekPolicyTest {
    private fun sample(index: Int, key: Int) = SampleInfo(index, index * 40_000L, index == key, key)

    @Test
    fun firstSampleSeeks() = assertTrue(SeekPolicy.needsSeek(sample(0, 0), -1))

    @Test
    fun sequentialFramesNeverSeek() {
        assertFalse(SeekPolicy.needsSeek(sample(1, 0), 0))
        assertFalse(SeekPolicy.needsSeek(sample(30, 30), 29))
        assertFalse(SeekPolicy.needsSeek(sample(31, 30), 30))
    }

    @Test
    fun skippingWholeGopSeeksForward() = assertTrue(SeekPolicy.needsSeek(sample(95, 90), 10))

    @Test
    fun sameGopFurtherAheadDecodesThrough() = assertFalse(SeekPolicy.needsSeek(sample(20, 0), 5))

    @Test
    fun backwardSampleSeeks() = assertTrue(SeekPolicy.needsSeek(sample(5, 0), 40))

    @Test
    fun keyframeJustAheadDecodesThrough() = assertFalse(SeekPolicy.needsSeek(sample(35, 32), 28))

    @Test
    fun distantKeyframeSeeks() = assertTrue(SeekPolicy.needsSeek(sample(60, 50), 30))
}
