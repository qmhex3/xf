package com.frameout.engine.decode

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

class GifFrameDecoder(private val context: Context) : AnimatedFrameSource {
    private var compositor: GifCompositor? = null
    private var reader: PfdRangeReader? = null
    private var timeline: List<Long> = emptyList()
    private var durationUs: Long = 0L
    private var width: Int = 0
    private var height: Int = 0

    override fun open(uri: Uri) {
        close()
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Cannot open GIF")
        val source = PfdRangeReader(descriptor)
        try {
            val starts = GifTimeline.read(PfdInputStream(descriptor).buffered())
            val parsed = GifIndexParser(PfdInputStream(descriptor).buffered()).parse()
            if (parsed.width.toLong() * parsed.height.toLong() * 4L * CANVAS_COPIES > MEMORY_BUDGET_BYTES) error("GIF canvas too large")
            reader = source
            compositor = GifCompositor(parsed) { offset, length -> source.read(offset, length) }
            timeline = starts
            durationUs = estimateDuration(starts)
            width = parsed.width
            height = parsed.height
        } catch (e: Throwable) {
            source.close()
            throw e
        }
    }

    override fun frameCount(): Int = timeline.size.coerceAtLeast(1)
    override fun videoWidth(): Int = width
    override fun videoHeight(): Int = height
    override fun durationUs(): Long = durationUs
    override fun ptsUs(index: Int): Long = if (timeline.isEmpty()) 0L else timeline[index.coerceIn(0, timeline.lastIndex)]

    override fun decodeFrame(index: Int): Pair<Long, Bitmap>? {
        val source = compositor ?: return null
        val safe = index.coerceIn(0, timeline.lastIndex)
        val pixels = source.render(safe).copyOf()
        AlphaFlatten.pixels(pixels)
        return timeline[safe] to Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    override fun close() {
        reader?.close()
        reader = null
        compositor = null
        timeline = emptyList()
        durationUs = 0L
        width = 0
        height = 0
    }

    private fun estimateDuration(starts: List<Long>): Long {
        if (starts.size <= 1) return 10_000L
        val lastGap = (starts.last() - starts[starts.lastIndex - 1]).coerceAtLeast(10_000L)
        return starts.last() + lastGap
    }

    private companion object {
        const val MEMORY_BUDGET_BYTES = 96L * 1024L * 1024L
        const val CANVAS_COPIES = 3L
    }
}
