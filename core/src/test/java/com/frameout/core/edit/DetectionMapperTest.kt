package com.frameout.core.edit

import com.frameout.core.quality.NormBox
import com.frameout.core.quality.NormBoxCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DetectionMapperTest {
    private val box = NormBox(0.1f, 0.2f, 0.3f, 0.4f)

    private fun assertBox(expected: NormBox, actual: NormBox?) {
        assertNotNull(actual)
        assertEquals(expected.l, actual!!.l, 0.0001f)
        assertEquals(expected.t, actual.t, 0.0001f)
        assertEquals(expected.r, actual.r, 0.0001f)
        assertEquals(expected.b, actual.b, 0.0001f)
    }

    @Test
    fun identityKeepsTheBox() = assertBox(box, DetectionMapper.map(box, FrameEdit(1)))

    @Test
    fun rotate90MovesTopLeftToTopRight() =
        assertBox(NormBox(0.6f, 0.1f, 0.8f, 0.3f), DetectionMapper.map(box, FrameEdit(1, rotation = 90)))

    @Test
    fun rotate180MirrorsBothAxes() =
        assertBox(NormBox(0.7f, 0.6f, 0.9f, 0.8f), DetectionMapper.map(box, FrameEdit(1, rotation = 180)))

    @Test
    fun rotate270MovesTopLeftToBottomLeft() =
        assertBox(NormBox(0.2f, 0.7f, 0.4f, 0.9f), DetectionMapper.map(box, FrameEdit(1, rotation = 270)))

    @Test
    fun flipsMirrorOneAxis() {
        assertBox(NormBox(0.7f, 0.2f, 0.9f, 0.4f), DetectionMapper.map(box, FrameEdit(1, flipH = true)))
        assertBox(NormBox(0.1f, 0.6f, 0.3f, 0.8f), DetectionMapper.map(box, FrameEdit(1, flipV = true)))
    }

    @Test
    fun cropRebasesTheBoxToTheCroppedImage() =
        assertBox(NormBox(0.2f, 0.4f, 0.6f, 0.8f), DetectionMapper.map(box, FrameEdit(1, cropL = 0f, cropT = 0f, cropR = 0.5f, cropB = 0.5f)))

    @Test
    fun boxOutsideTheCropDisappears() =
        assertNull(DetectionMapper.map(box, FrameEdit(1, cropL = 0.5f, cropT = 0.5f, cropR = 1f, cropB = 1f)))

    @Test
    fun boxMostlyCutOffIsDropped() =
        assertNull(DetectionMapper.map(box, FrameEdit(1, cropL = 0.28f, cropT = 0f, cropR = 1f, cropB = 1f)))

    @Test
    fun codecRoundTripsAndRejectsGarbage() {
        val boxes = listOf(box, NormBox(0f, 0f, 1f, 1f))
        assertEquals(boxes, NormBoxCodec.decode(NormBoxCodec.encode(boxes)))
        assertEquals(emptyList<NormBox>(), NormBoxCodec.decode("1,2,3;a,b,c,d;0.5,0.5,0.2,0.2"))
        assertEquals(emptyList<NormBox>(), NormBoxCodec.decode(""))
    }

    @Test
    fun codecCapsTheNumberOfBoxes() {
        val many = List(20) { NormBox(0.1f, 0.1f, 0.2f + it * 0.01f, 0.3f) }
        assertEquals(NormBoxCodec.MAX_PER_TAG, NormBoxCodec.decode(NormBoxCodec.encode(many)).size)
    }
}
