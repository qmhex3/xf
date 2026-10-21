package com.frameout.core.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeMarkTest {
    private val eps = 0.0005f

    @Test
    fun everyKindSurvivesTheCodec() {
        val marks = ShapeKind.entries.mapIndexed { i, kind ->
            ShapeMark(
                kind, -65536 + i, 0.0125f, 0.1f, 0.2f, 0.6f, 0.7f,
                fill = ShapeFill.entries[i % ShapeFill.entries.size], dash = i % 2 == 0, opacity = 0.8f, number = i + 1
            )
        }
        assertEquals(marks, MarkCodec.decode(MarkCodec.encode(marks)))
    }

    @Test
    fun brokenShapeLinesAreSkipped() {
        val stored = "H|Nope|1|0.01|0,0,1,1|None|0|1|1\nH|Arrow|1|0.01|0,0,1|None|0|1|1"
        assertTrue(MarkCodec.decode(stored).isEmpty())
    }

    @Test
    fun shapesMixWithOtherMarks() {
        val marks = listOf<EditMark>(
            StrokeMark(-65536, 0.0125f, listOf(MarkPoint(0.1f, 0.2f))),
            ShapeMark(ShapeKind.Number, -1, 0.01f, 0.2f, 0.2f, 0.3f, 0.3f, number = 4),
            CoverMark(0.1f, 0.2f, 0.6f, 0.7f, CoverStyle.Blur)
        )
        assertEquals(marks, MarkCodec.decode(MarkCodec.encode(marks)))
    }

    @Test
    fun shapeRoundTripThroughRotatedSpaceKeepsPointsAndWidth() {
        val space = MarkSpace(400, 300, rotation = 90, flipH = true, cropL = 0.1f, cropT = 0.1f, cropR = 0.9f, cropB = 0.9f)
        val mark = ShapeMark(ShapeKind.TaperArrow, -1, 0.01f, 0.3f, 0.4f, 0.7f, 0.6f)
        val back = space.toSource(space.toOutput(mark)) as ShapeMark
        assertEquals(mark.x1, back.x1, eps)
        assertEquals(mark.y1, back.y1, eps)
        assertEquals(mark.x2, back.x2, eps)
        assertEquals(mark.y2, back.y2, eps)
        assertEquals(mark.width, back.width, eps)
    }

    @Test
    fun kindTraitsAreConsistent() {
        assertTrue(ShapeKind.Arrow.pointBased)
        assertFalse(ShapeKind.Rect.pointBased)
        assertTrue(ShapeKind.Number.marker && ShapeKind.Number.radial)
        assertTrue(ShapeKind.Rect.supportsFill)
        assertFalse(ShapeKind.TaperArrow.supportsDash)
        ShapeKind.entries.filter { it.marker }.forEach { assertTrue(it.radial) }
    }
}
