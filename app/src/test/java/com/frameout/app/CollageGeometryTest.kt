package com.frameout.app

import com.frameout.core.collage.COLLAGE_GAP_MAX
import com.frameout.core.collage.COLLAGE_MAX_ZOOM
import com.frameout.core.collage.COLLAGE_REFERENCE_WIDTH
import com.frameout.core.collage.COLLAGE_RESOLUTIONS
import com.frameout.core.collage.CellCrop
import com.frameout.core.collage.CollageAspect
import com.frameout.core.collage.CollageTemplates
import com.frameout.core.collage.CropGeometry
import com.frameout.core.collage.collageMemoryPixels
import com.frameout.core.collage.collageOutputSize
import com.frameout.core.collage.collageUnit
import com.frameout.core.collage.sanitizeCollageGap
import com.frameout.core.collage.toPx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollageGeometryTest {
    private val big = 24_000_000L
    private val source = CropGeometry(4000f, 3000f, 1000f, 1000f)

    @Test
    fun aspectKeysResolveAndFallBackToTemplate() {
        assertEquals(CollageAspect.Wide, CollageAspect.fromKey("16:9"))
        assertEquals(CollageAspect.Story, CollageAspect.fromKey("9:16"))
        assertEquals(CollageAspect.Template, CollageAspect.fromKey("nonsense"))
        assertEquals(CollageAspect.Template, CollageAspect.fromKey(null))
    }

    @Test
    fun templateAspectFollowsOverrideAndRestores() {
        val grid = CollageTemplates.all.first { it.id == "2x1" }
        assertEquals(2f, grid.aspect, 0.0001f)
        assertEquals(16f / 9f, grid.withAspect(CollageAspect.Wide).aspect, 0.0001f)
        assertEquals(0.8f, grid.withAspect(CollageAspect.Portrait).aspect, 0.0001f)
        assertEquals(2f, grid.withAspect(CollageAspect.Wide).withAspect(CollageAspect.Template).aspect, 0.0001f)
    }

    @Test
    fun squareGridCellsBecomeTallOnStoryCanvas() {
        val grid = CollageTemplates.all.first { it.id == "2x2" }.withAspect(CollageAspect.Story)
        val size = collageOutputSize(1080, grid.aspect, big)
        assertEquals(1080 to 1920, size)
        val cell = grid.cells[0].toPx(size.first.toFloat(), size.second.toFloat(), 0f)
        assertEquals(540f, cell.width, 0.01f)
        assertEquals(960f, cell.height, 0.01f)
    }

    @Test
    fun cellsStayInsideCanvasForEveryTemplateAndAspect() {
        for (t in CollageTemplates.all) for (a in CollageAspect.entries) {
            val template = t.withAspect(a)
            val size = collageOutputSize(1536, template.aspect, big)
            template.cells.forEach { c ->
                val r = c.toPx(size.first.toFloat(), size.second.toFloat(), 12f)
                assertTrue(r.left >= 0f && r.top >= 0f && r.right <= size.first + 0.01f && r.bottom <= size.second + 0.01f)
            }
        }
    }

    @Test
    fun outputSizeFollowsAspectAndResolution() {
        assertEquals(1080 to 1080, collageOutputSize(1080, 1f, big))
        assertEquals(1536 to 1920, collageOutputSize(1536, 0.8f, big))
        assertEquals(1080 to 607, collageOutputSize(1080, 16f / 9f, big))
        assertEquals(2048 to 10240, collageOutputSize(2048, 0.2f, big))
    }

    @Test
    fun outputSizeIsCappedByMemoryAndNeverBelowMinimum() {
        val capped = collageOutputSize(2048, 0.2f, 4_000_000L)
        assertTrue(capped.first < 2048)
        assertTrue(capped.first.toLong() * capped.second <= 4_000_000L)
        assertEquals(320, collageOutputSize(10, 1f, big).first)
        assertEquals(320, collageOutputSize(2048, 1f, 1L).first)
    }

    @Test
    fun memoryBudgetIsClamped() {
        assertEquals(4_000_000L, collageMemoryPixels(0L))
        assertEquals(24_000_000L, collageMemoryPixels(4_000_000_000L))
        assertEquals(16_000_000L, collageMemoryPixels(128_000_000L))
    }

    @Test
    fun spacingKeepsTheSameFractionOfWidthAtEveryResolution() {
        val reference = 20f / COLLAGE_REFERENCE_WIDTH
        COLLAGE_RESOLUTIONS.forEach { width ->
            assertEquals(reference, 20f * collageUnit(width.toFloat()) / width, 0.000001f)
        }
        assertTrue(COLLAGE_RESOLUTIONS.contains(1536))
    }

    @Test
    fun gapIsSanitized() {
        assertEquals(0, sanitizeCollageGap(-5))
        assertEquals(COLLAGE_GAP_MAX, sanitizeCollageGap(900))
        assertEquals(12, sanitizeCollageGap(12))
    }

    @Test
    fun defaultCropIsCenterCrop() {
        val w = source.pixelWindow(CellCrop())
        assertEquals(500, w.left)
        assertEquals(3500, w.right)
        assertEquals(0, w.top)
        assertEquals(3000, w.bottom)
    }

    @Test
    fun zoomShrinksTheWindowAroundTheCenter() {
        val w = source.window(CellCrop(scale = 2f))
        assertEquals(1500f, w.right - w.left, 0.01f)
        assertEquals(1500f, w.bottom - w.top, 0.01f)
        assertEquals(2000f, (w.left + w.right) / 2f, 0.01f)
        assertEquals(1500f, (w.top + w.bottom) / 2f, 0.01f)
    }

    @Test
    fun positiveOffsetMovesTheImageRightSoTheWindowMovesLeft() {
        val w = source.window(CellCrop(scale = 2f, offsetX = 0.1f))
        assertEquals(1100f, w.left, 0.01f)
        assertEquals(2600f, w.right, 0.01f)
    }

    @Test
    fun offsetsAreClampedSoTheWindowStaysInsideTheSource() {
        listOf(-9f, 9f).forEach { extreme ->
            val w = source.window(CellCrop(scale = 3f, offsetX = extreme, offsetY = extreme))
            assertTrue(w.left >= -0.01f && w.top >= -0.01f && w.right <= 4000.01f && w.bottom <= 3000.01f)
        }
    }

    @Test
    fun axisWithoutSlackCannotPanAtBaseScale() {
        val c = source.clamp(CellCrop(scale = 1f, offsetX = 0.2f, offsetY = 0.2f))
        assertEquals(0f, c.offsetY, 0.0001f)
        assertTrue(c.offsetX > 0f)
        assertEquals(1f / 6f, source.clamp(CellCrop(1f, 9f, 0f)).offsetX, 0.0001f)
    }

    @Test
    fun scaleIsClampedBetweenOneAndMax() {
        assertEquals(1f, source.clamp(CellCrop(scale = 0.2f)).scale, 0.0001f)
        assertEquals(COLLAGE_MAX_ZOOM, source.clamp(CellCrop(scale = 99f)).scale, 0.0001f)
        assertEquals(COLLAGE_MAX_ZOOM, source.transform(CellCrop(), 0f, 0f, 100f, 500f, 500f).scale, 0.0001f)
        assertEquals(1f, source.transform(CellCrop(), 0f, 0f, 0.1f, 500f, 500f).scale, 0.0001f)
    }

    @Test
    fun pinchKeepsTheSourcePointUnderTheFingers() {
        val start = CellCrop(scale = 1.5f, offsetX = 0.1f, offsetY = 0f)
        val fx = 800f
        val fy = 300f
        val before = sourceAt(source.window(start), fx, fy)
        val after = sourceAt(source.window(source.transform(start, 0f, 0f, 1.6f, fx, fy)), fx, fy)
        assertEquals(before.first, after.first, 0.5f)
        assertEquals(before.second, after.second, 0.5f)
    }

    @Test
    fun panMovesTheOffsetByTheFractionOfTheCell() {
        val zoomed = CellCrop(scale = 3f)
        val moved = source.transform(zoomed, 100f, -50f, 1f, 500f, 500f)
        assertEquals(0.1f, moved.offsetX, 0.0001f)
        assertEquals(-0.05f, moved.offsetY, 0.0001f)
        assertEquals(3f, moved.scale, 0.0001f)
    }

    @Test
    fun previewTransformAndOutputWindowAgree() {
        val crop = source.clamp(CellCrop(scale = 2.5f, offsetX = 0.2f, offsetY = -0.3f))
        val base = maxOf(1000f / 4000f, 1000f / 3000f)
        val window = source.window(crop)
        listOf(0f to 0f, 1000f to 1000f, 250f to 700f).forEach { (dx, dy) ->
            val vx = 500f + (dx - crop.offsetX * 1000f - 500f) / crop.scale
            val vy = 500f + (dy - crop.offsetY * 1000f - 500f) / crop.scale
            val previewX = 2000f + (vx - 500f) / base
            val previewY = 1500f + (vy - 500f) / base
            val out = sourceAt(window, dx, dy)
            assertEquals(previewX, out.first, 0.01f)
            assertEquals(previewY, out.second, 0.01f)
        }
    }

    @Test
    fun pixelWindowIsAlwaysAtLeastOnePixelAndInsideTheBitmap() {
        val tiny = CropGeometry(10f, 10f, 1000f, 1000f)
        listOf(CellCrop(), CellCrop(5f, 3f, -3f), CellCrop(1f, -9f, 9f)).forEach { crop ->
            val w = tiny.pixelWindow(crop)
            assertTrue(w.left >= 0 && w.top >= 0 && w.right <= 10 && w.bottom <= 10)
            assertTrue(w.right > w.left && w.bottom > w.top)
        }
    }

    private fun sourceAt(w: com.frameout.core.collage.SourceWindow, cellX: Float, cellY: Float): Pair<Float, Float> =
        (w.left + cellX / 1000f * (w.right - w.left)) to (w.top + cellY / 1000f * (w.bottom - w.top))
}
