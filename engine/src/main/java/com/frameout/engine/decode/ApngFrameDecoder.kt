package com.frameout.engine.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import com.frameout.codecpack.ApngFrame
import com.frameout.codecpack.ApngIndex
import com.frameout.codecpack.ApngParser

class ApngFrameDecoder(private val context: Context) : AnimatedFrameSource {
    private var index: ApngIndex? = null
    private var reader: PfdRangeReader? = null
    private var starts = LongArray(0)
    private var total = 0L
    private var canvasBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var snapshot: Bitmap? = null
    private var rendered = -1
    private var base = 0
    private var resetPoints = IntArray(0)
    private val sourcePaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }
    private val overPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER) }
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun open(uri: Uri) {
        close()
        val parsed = context.contentResolver.openInputStream(uri)?.buffered()?.use { ApngParser.parse(it) } ?: error("Cannot open APNG")
        if (parsed.width.toLong() * parsed.height.toLong() * 4L * CANVAS_COPIES > MEMORY_BUDGET_BYTES) error("APNG canvas too large")
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Cannot open APNG")
        index = parsed
        reader = PfdRangeReader(descriptor)
        starts = parsed.startsUs()
        total = parsed.durationUs()
        resetPoints = parsed.frames.indices.filter { it == 0 || coversCanvas(parsed, parsed.frames[it]) }.toIntArray()
    }

    override fun frameCount(): Int = index?.frames?.size ?: 0
    override fun videoWidth(): Int = index?.width ?: 0
    override fun videoHeight(): Int = index?.height ?: 0
    override fun durationUs(): Long = total
    override fun ptsUs(index: Int): Long = if (starts.isEmpty()) 0L else starts[index.coerceIn(0, starts.lastIndex)]

    override fun decodeFrame(index: Int): Pair<Long, Bitmap>? {
        val parsed = this.index ?: return null
        val safe = index.coerceIn(0, parsed.frames.lastIndex)
        ensureCanvas(parsed)
        val start = ResetPoints.lastAtOrBefore(resetPoints, safe)
        if (safe < rendered || start > rendered) reset(start)
        while (rendered < safe) advance(parsed)
        val copy = canvasBitmap?.let { AlphaFlatten.bitmap(it) } ?: return null
        return starts[safe] to copy
    }

    override fun close() {
        reader?.close()
        reader = null
        index = null
        starts = LongArray(0)
        total = 0L
        canvasBitmap?.recycle()
        snapshot?.recycle()
        canvasBitmap = null
        snapshot = null
        canvas = null
        rendered = -1
        base = 0
        resetPoints = IntArray(0)
    }

    private fun reset(start: Int) {
        canvasBitmap?.eraseColor(0)
        rendered = start - 1
        base = start
    }

    private fun coversCanvas(parsed: ApngIndex, frame: ApngFrame): Boolean =
        frame.blend == ApngIndex.BLEND_SOURCE && frame.x == 0 && frame.y == 0 &&
            frame.width >= parsed.width && frame.height >= parsed.height && frame.dispose != ApngIndex.DISPOSE_PREVIOUS

    private fun ensureCanvas(parsed: ApngIndex) {
        if (canvasBitmap != null) return
        val bitmap = Bitmap.createBitmap(parsed.width, parsed.height, Bitmap.Config.ARGB_8888)
        canvasBitmap = bitmap
        canvas = Canvas(bitmap)
    }

    private fun advance(parsed: ApngIndex) {
        val c = canvas ?: return
        val next = rendered + 1
        if (rendered >= base) disposePrevious(c, parsed.frames[rendered], rendered)
        val frame = parsed.frames[next]
        if (frame.dispose == ApngIndex.DISPOSE_PREVIOUS) saveSnapshot(parsed)
        drawFrame(c, parsed, frame)
        rendered = next
    }

    private fun disposePrevious(c: Canvas, previous: ApngFrame, previousIndex: Int) {
        val region = Rect(previous.x, previous.y, previous.x + previous.width, previous.y + previous.height)
        val saved = snapshot
        when {
            previous.dispose == ApngIndex.DISPOSE_BACKGROUND -> c.drawRect(region, clearPaint)
            previous.dispose == ApngIndex.DISPOSE_PREVIOUS && (previousIndex == 0 || saved == null) -> c.drawRect(region, clearPaint)
            previous.dispose == ApngIndex.DISPOSE_PREVIOUS && saved != null -> c.drawBitmap(saved, 0f, 0f, sourcePaint)
        }
    }

    private fun saveSnapshot(parsed: ApngIndex) {
        val source = canvasBitmap ?: return
        val target = snapshot ?: Bitmap.createBitmap(parsed.width, parsed.height, Bitmap.Config.ARGB_8888).also { snapshot = it }
        Canvas(target).drawBitmap(source, 0f, 0f, sourcePaint)
    }

    private fun drawFrame(c: Canvas, parsed: ApngIndex, frame: ApngFrame) {
        val source = reader ?: return
        val png = ApngFramePng.build(parsed, frame) { offset, length -> source.read(offset, length) }
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap = BitmapFactory.decodeByteArray(png, 0, png.size, options) ?: error("APNG frame decode failed")
        try {
            val paint = if (frame.blend == ApngIndex.BLEND_OVER) overPaint else sourcePaint
            c.drawBitmap(bitmap, frame.x.toFloat(), frame.y.toFloat(), paint)
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val MEMORY_BUDGET_BYTES = 96L * 1024L * 1024L
        const val CANVAS_COPIES = 4L
    }
}
