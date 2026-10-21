package com.frameout.codecpack

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ApngParserTest {
    private val f = ApngFixture
    private val defaultData = byteArrayOf(1, 2, 3, 4)
    private val frameOne = byteArrayOf(10, 11, 12, 13, 14)
    private val frameTwo = byteArrayOf(20, 21, 22)

    private fun hiddenDefault(): ByteArray = f.assemble(
        f.ihdr(8, 6), f.actl(2), f.chunk("IDAT", defaultData),
        f.fctl(0, 8, 6, 0, 0, 1, 10, 0, 0), f.fdat(1, frameOne),
        f.fctl(2, 4, 3, 2, 1, 1, 10, 1, 1), f.fdat(3, frameTwo)
    )

    private fun visibleDefault(): ByteArray = f.assemble(
        f.ihdr(8, 6), f.actl(2),
        f.fctl(0, 8, 6, 0, 0, 1, 10, 0, 0), f.chunk("IDAT", defaultData),
        f.fctl(1, 8, 6, 0, 0, 1, 10, 0, 0), f.fdat(2, frameTwo)
    )

    @Test
    fun detectsAnimationControlBeforeImageData() {
        assertTrue(ApngParser.isAnimated(ByteArrayInputStream(hiddenDefault())))
    }

    @Test
    fun plainPngIsNotAnimated() {
        val png = f.assemble(f.ihdr(8, 6), f.chunk("IDAT", defaultData))
        assertFalse(ApngParser.isAnimated(ByteArrayInputStream(png)))
    }

    @Test
    fun garbageAndTruncatedInputsAreNotAnimated() {
        assertFalse(ApngParser.isAnimated(ByteArrayInputStream(ByteArray(40) { 7 })))
        assertFalse(ApngParser.isAnimated(ByteArrayInputStream(hiddenDefault().copyOf(12))))
        assertFalse(ApngParser.isAnimated(ByteArrayInputStream(ByteArray(0))))
    }

    @Test
    fun hiddenDefaultImageIsExcludedFromFrames() {
        val index = ApngParser.parse(ByteArrayInputStream(hiddenDefault()))
        assertEquals(2, index.frames.size)
        assertEquals(8, index.width)
        assertEquals(6, index.height)
    }

    @Test
    fun visibleDefaultImageBecomesFirstFrame() {
        val bytes = visibleDefault()
        val index = ApngParser.parse(ByteArrayInputStream(bytes))
        assertEquals(2, index.frames.size)
        val range = index.frames[0].data.single()
        assertArrayEquals(defaultData, bytes.copyOfRange(range.offset.toInt(), range.offset.toInt() + range.length))
    }

    @Test
    fun fdatRangesPointAtPayloadWithoutSequenceNumber() {
        val bytes = hiddenDefault()
        val index = ApngParser.parse(ByteArrayInputStream(bytes))
        val first = index.frames[0].data.single()
        val second = index.frames[1].data.single()
        assertArrayEquals(frameOne, bytes.copyOfRange(first.offset.toInt(), first.offset.toInt() + first.length))
        assertArrayEquals(frameTwo, bytes.copyOfRange(second.offset.toInt(), second.offset.toInt() + second.length))
    }

    @Test
    fun readsRegionDisposeAndBlendOfEachFrame() {
        val frame = ApngParser.parse(ByteArrayInputStream(hiddenDefault())).frames[1]
        assertEquals(2, frame.x)
        assertEquals(1, frame.y)
        assertEquals(4, frame.width)
        assertEquals(3, frame.height)
        assertEquals(ApngIndex.DISPOSE_BACKGROUND, frame.dispose)
        assertEquals(ApngIndex.BLEND_OVER, frame.blend)
    }

    @Test
    fun delaysUseFractionsAndClampToMinimum() {
        val bytes = f.assemble(
            f.ihdr(2, 2), f.actl(4),
            f.fctl(0, 2, 2, 0, 0, 1, 10, 0, 0), f.chunk("IDAT", defaultData),
            f.fctl(1, 2, 2, 0, 0, 0, 0, 0, 0), f.fdat(2, frameOne),
            f.fctl(3, 2, 2, 0, 0, 5, 0, 0, 0), f.fdat(4, frameOne),
            f.fctl(5, 2, 2, 0, 0, 1, 25, 0, 0), f.fdat(6, frameOne)
        )
        val index = ApngParser.parse(ByteArrayInputStream(bytes))
        assertEquals(listOf(100L, 10L, 50L, 40L), index.frames.map { it.delayMs })
        assertArrayEquals(longArrayOf(0L, 100_000L, 110_000L, 160_000L), index.startsUs())
        assertEquals(200_000L, index.durationUs())
    }

    @Test
    fun keepsColorChunksBeforeImageDataInOrder() {
        val bytes = f.assemble(
            f.ihdr(2, 2), f.chunk("PLTE", ByteArray(6)), f.chunk("tRNS", byteArrayOf(1, 2)),
            f.chunk("tEXt", byteArrayOf(65, 0, 66)), f.actl(1),
            f.fctl(0, 2, 2, 0, 0, 1, 10, 0, 0), f.chunk("IDAT", defaultData),
            f.chunk("gAMA", ByteArray(4))
        )
        val index = ApngParser.parse(ByteArrayInputStream(bytes))
        assertEquals(listOf("PLTE", "tRNS"), index.shared.map { it.type })
    }

    @Test
    fun combinesSeveralDataChunksOfOneFrame() {
        val bytes = f.assemble(
            f.ihdr(2, 2), f.actl(1),
            f.fctl(0, 2, 2, 0, 0, 1, 10, 0, 0), f.chunk("IDAT", frameOne), f.chunk("IDAT", frameTwo)
        )
        val frame = ApngParser.parse(ByteArrayInputStream(bytes)).frames.single()
        assertEquals(listOf(frameOne.size, frameTwo.size), frame.data.map { it.length })
    }

    @Test
    fun rejectsFrameOutsideCanvas() {
        val bytes = f.assemble(f.ihdr(4, 4), f.actl(1), f.fctl(0, 4, 4, 1, 0, 1, 10, 0, 0), f.chunk("IDAT", frameOne))
        try {
            ApngParser.parse(ByteArrayInputStream(bytes))
            fail()
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun rejectsFrameWithoutData() {
        val bytes = f.assemble(f.ihdr(4, 4), f.actl(1), f.fctl(0, 4, 4, 0, 0, 1, 10, 0, 0))
        try {
            ApngParser.parse(ByteArrayInputStream(bytes))
            fail()
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun rejectsNonPngInput() {
        try {
            ApngParser.parse(ByteArrayInputStream(ByteArray(64) { 1 }))
            fail()
        } catch (_: IllegalStateException) {
        }
    }
}
