package com.frameout.core.edit

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkSpaceTest {
    private val eps = 1e-4f

    private fun geometries(): List<MarkSpace> {
        val out = ArrayList<MarkSpace>()
        for (turn in listOf(0, 90, 180, 270)) for (h in listOf(false, true)) for (v in listOf(false, true)) {
            out += MarkSpace(400, 300, turn, h, v, 0.1f, 0.2f, 0.9f, 0.7f)
            out += MarkSpace(400, 300, turn, h, v)
        }
        return out
    }

    @Test
    fun identityKeepsMarksUnchanged() {
        val space = MarkSpace(400, 300)
        val mark = StrokeMark(-1, 0.02f, listOf(MarkPoint(0.2f, 0.3f)))
        assertEquals(mark, space.toOutput(mark))
    }

    @Test
    fun quarterTurnMapsPointsLikeTheCropMath() {
        val space = MarkSpace(100, 200, rotation = 90)
        val p = space.toOutput(MarkPoint(0.25f, 0.5f))
        assertEquals(0.5f, p.x, eps)
        assertEquals(0.25f, p.y, eps)
        assertEquals(200, space.outputWidth)
        assertEquals(100, space.outputHeight)
    }

    @Test
    fun cropShiftsAndRescalesPoints() {
        val space = MarkSpace(400, 300, cropL = 0.25f, cropT = 0f, cropR = 0.75f, cropB = 1f)
        val p = space.toOutput(MarkPoint(0.5f, 0.5f))
        assertEquals(0.5f, p.x, eps)
        assertEquals(0.5f, p.y, eps)
        val corner = space.toOutput(MarkPoint(0.25f, 0f))
        assertEquals(0f, corner.x, eps)
    }

    @Test
    fun pointsRoundTripForEveryGeometry() {
        geometries().forEach { space ->
            val p = MarkPoint(0.4f, 0.55f)
            val back = space.toSource(space.toOutput(p))
            assertEquals(p.x, back.x, eps)
            assertEquals(p.y, back.y, eps)
        }
    }

    @Test
    fun strokeAndTextKeepTheirPixelSize() {
        geometries().forEach { space ->
            val stroke = space.toOutput(StrokeMark(-1, 0.02f, listOf(MarkPoint(0.5f, 0.5f)))) as StrokeMark
            val outMin = minOf(space.outputWidth, space.outputHeight)
            assertEquals(0.02f * space.sourceMin, stroke.width * outMin, 0.01f)
            val text = space.textToOutput(TextMark.Default.copy(text = "x", size = 0.1f))
            assertEquals(0.1f * space.sourceMin, text.size * outMin, 0.01f)
        }
    }

    @Test
    fun uprightTextOnTheCanvasStaysUprightWhateverTheGeometry() {
        geometries().forEach { space ->
            val shown = TextMark.Default.copy(text = "x", x = 0.3f, y = 0.6f, size = 0.08f, rotation = 0f)
            val stored = space.textToSource(shown)
            val again = space.textToOutput(stored)
            assertEquals(0f, again.rotation, eps)
            assertEquals(shown.x, again.x, eps)
            assertEquals(shown.y, again.y, eps)
            assertEquals(shown.size, again.size, eps)
        }
    }

    @Test
    fun textStoredUprightOnARotatedCanvasCompensatesTheTurn() {
        val space = MarkSpace(400, 300, rotation = 90)
        val stored = space.textToSource(TextMark.Default.copy(text = "x", rotation = 0f))
        assertEquals(-90f, stored.rotation, eps)
    }

    @Test
    fun coverRectangleStaysARectangle() {
        val space = MarkSpace(400, 300, rotation = 270, flipH = true)
        val cover = CoverMark(0.1f, 0.2f, 0.4f, 0.6f, CoverStyle.Blur)
        val shown = space.toOutput(cover) as CoverMark
        assertEquals(true, shown.left < shown.right && shown.top < shown.bottom)
        val back = space.toSource(shown) as CoverMark
        assertEquals(cover.left, back.left, eps)
        assertEquals(cover.bottom, back.bottom, eps)
    }
}
