package com.frameout.app

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.engine.decode.ApngFrameDecoder
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApngFrameDecoderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val files = mutableListOf<File>()
    private val none = 0
    private val background = 1
    private val previous = 2
    private val source = 0
    private val over = 1

    @After
    fun cleanup() {
        files.forEach { it.delete() }
    }

    private fun solid(w: Int, h: Int, color: Int): () -> Bitmap = {
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { it.eraseColor(color) }
    }

    private fun spec(x: Int, y: Int, w: Int, h: Int, color: Int, dispose: Int = none, blend: Int = source, num: Int = 1, den: Int = 10) =
        ApngFrameSpec(x, y, dispose, blend, num, den, solid(w, h, color))

    private fun open(vararg frames: ApngFrameSpec): ApngFrameDecoder {
        val file = File(context.cacheDir, "decoder_${System.nanoTime()}.apng").also { files += it }
        ApngTestWriter.write(file, 4, 4, frames.size) { frames[it] }
        return ApngFrameDecoder(context).also { it.open(Uri.fromFile(file)) }
    }

    private fun pixel(decoder: ApngFrameDecoder, frame: Int, x: Int, y: Int): Int {
        val decoded = decoder.decodeFrame(frame)
        assertNotNull(decoded)
        val bitmap = decoded!!.second
        return bitmap.getPixel(x, y).also { bitmap.recycle() }
    }

    @Test
    fun reportsCanvasSizeAndFrameCount() {
        val decoder = open(spec(0, 0, 4, 4, Color.RED), spec(0, 0, 4, 4, Color.BLUE))
        assertEquals(4, decoder.videoWidth())
        assertEquals(4, decoder.videoHeight())
        assertEquals(2, decoder.frameCount())
        decoder.close()
    }

    @Test
    fun timelineFollowsFrameDelays() {
        val decoder = open(spec(0, 0, 4, 4, Color.RED, den = 10), spec(0, 0, 4, 4, Color.BLUE, num = 1, den = 4), spec(0, 0, 4, 4, Color.GREEN))
        assertEquals(0L, decoder.ptsUs(0))
        assertEquals(100_000L, decoder.ptsUs(1))
        assertEquals(350_000L, decoder.ptsUs(2))
        assertEquals(450_000L, decoder.durationUs())
        decoder.close()
    }

    @Test
    fun partialFrameKeepsPixelsOutsideItsRegion() {
        val decoder = open(spec(0, 0, 4, 4, Color.RED), spec(1, 1, 2, 2, Color.BLUE))
        assertEquals(Color.BLUE, pixel(decoder, 1, 1, 1))
        assertEquals(Color.RED, pixel(decoder, 1, 0, 0))
        assertEquals(Color.RED, pixel(decoder, 1, 3, 3))
        decoder.close()
    }

    @Test
    fun disposeBackgroundClearsPreviousRegion() {
        val decoder = open(spec(0, 0, 4, 4, Color.RED, dispose = background), spec(0, 0, 2, 2, Color.GREEN))
        assertEquals(Color.GREEN, pixel(decoder, 1, 0, 0))
        assertEquals(Color.WHITE, pixel(decoder, 1, 3, 3))
        decoder.close()
    }

    @Test
    fun disposePreviousRestoresCanvasBeforeThatFrame() {
        val decoder = open(
            spec(0, 0, 4, 4, Color.RED),
            spec(0, 0, 2, 2, Color.BLUE, dispose = previous),
            spec(2, 2, 2, 2, Color.GREEN)
        )
        assertEquals(Color.BLUE, pixel(decoder, 1, 0, 0))
        assertEquals(Color.RED, pixel(decoder, 2, 0, 0))
        assertEquals(Color.GREEN, pixel(decoder, 2, 3, 3))
        decoder.close()
    }

    @Test
    fun blendOverKeepsBackgroundUnderTransparentPixels() {
        val decoder = open(spec(0, 0, 4, 4, Color.RED), spec(0, 0, 4, 4, Color.TRANSPARENT, blend = over))
        assertEquals(Color.RED, pixel(decoder, 1, 2, 2))
        decoder.close()
    }

    @Test
    fun blendSourceReplacesBackgroundWithTransparency() {
        val decoder = open(spec(0, 0, 4, 4, Color.RED), spec(0, 0, 4, 4, Color.TRANSPARENT, blend = source))
        assertEquals(Color.WHITE, pixel(decoder, 1, 2, 2))
        decoder.close()
    }

    @Test
    fun randomAccessMatchesSequentialDecoding() {
        val decoder = open(spec(0, 0, 4, 4, Color.RED), spec(1, 1, 2, 2, Color.BLUE, dispose = previous), spec(2, 2, 2, 2, Color.GREEN))
        val sequential = (0..2).map { pixel(decoder, it, 1, 1) to pixel(decoder, it, 3, 3) }
        val backwards = (2 downTo 0).map { pixel(decoder, it, 1, 1) to pixel(decoder, it, 3, 3) }.reversed()
        assertEquals(sequential, backwards)
        decoder.close()
    }

    @Test
    fun nearestFrameFollowsPresentationTime() {
        val decoder = open(spec(0, 0, 4, 4, Color.RED), spec(0, 0, 4, 4, Color.BLUE), spec(0, 0, 4, 4, Color.GREEN))
        val nearest = decoder.decodeNearest(190_000L)
        assertNotNull(nearest)
        assertEquals(200_000L, nearest!!.first)
        decoder.close()
    }
}
