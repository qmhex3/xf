package com.frameout.core.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import java.io.OutputStream

object ContactSheetImage {
    private const val BACKGROUND = 0xFF121212.toInt()
    private const val CELL = 0xFF1E1E1E.toInt()
    private const val INK = 0xFFE8E8E8.toInt()
    private const val MUTED = 0xFFA8A8A8.toInt()
    private const val JPEG_QUALITY = 92
    private const val ELLIPSIS = "…"

    suspend fun render(
        plan: ContactSheetLayout.Plan,
        sheet: Int,
        title: String,
        times: List<String>,
        options: ContactSheetLayout.Options,
        load: suspend (Int) -> Bitmap?
    ): Bitmap {
        val range = plan.sheets[sheet]
        val bitmap = Bitmap.createBitmap(plan.sheetWidth, plan.sheetHeight(sheet), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BACKGROUND)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        drawHeader(canvas, plan, sheet, title, paint)
        for ((slot, index) in range.withIndex()) {
            val left = plan.cellLeft(slot).toFloat()
            val top = plan.cellTop(slot).toFloat()
            paint.color = CELL
            canvas.drawRect(left, top, left + plan.cellW, top + plan.cellH, paint)
            val frame = load(index)
            if (frame != null) {
                val box = ContactSheetLayout.fit(frame.width, frame.height, plan.cellW, plan.cellH)
                canvas.drawBitmap(frame, null, RectF(left + box[0], top + box[1], left + box[2], top + box[3]), paint)
                frame.recycle()
            }
            drawCaption(canvas, plan, options, left, top + plan.cellH, index + 1, times.getOrNull(index).orEmpty(), paint)
        }
        return bitmap
    }

    fun write(bitmap: Bitmap, png: Boolean, out: OutputStream) {
        val format = if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        if (!bitmap.compress(format, if (png) 100 else JPEG_QUALITY, out)) error("Compress failed")
    }

    private fun drawHeader(canvas: Canvas, plan: ContactSheetLayout.Plan, sheet: Int, title: String, paint: Paint) {
        paint.color = INK
        paint.textSize = 34f
        paint.isFakeBoldText = true
        val suffix = if (plan.sheets.size > 1) "  ·  ${sheet + 1}/${plan.sheets.size}" else "  ·  ${plan.count}"
        val room = plan.sheetWidth - 2f * ContactSheetLayout.MARGIN
        val reserved = paint.measureText(suffix)
        val line = fit(title, paint, room - reserved) + suffix
        canvas.drawText(line, ContactSheetLayout.MARGIN.toFloat(), ContactSheetLayout.MARGIN + 38f, paint)
        paint.isFakeBoldText = false
    }

    private fun drawCaption(canvas: Canvas, plan: ContactSheetLayout.Plan, options: ContactSheetLayout.Options, left: Float, top: Float, number: Int, time: String, paint: Paint) {
        if (plan.captionH <= 0) return
        val first = if (options.showNumber) "#$number" else time.takeIf { options.showTime }.orEmpty()
        val second = if (options.showNumber && options.showTime) time else ""
        var size = plan.textSize
        paint.textSize = size
        while (size > 10f && paint.measureText(first) + paint.measureText(second) + size > plan.cellW) {
            size -= 1f
            paint.textSize = size
        }
        val baseline = top + plan.captionH * 0.72f
        paint.color = INK
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(first, left + 2f, baseline, paint)
        if (second.isNotEmpty()) {
            paint.color = MUTED
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(second, left + plan.cellW - 2f, baseline, paint)
            paint.textAlign = Paint.Align.LEFT
        }
    }

    private fun fit(text: String, paint: Paint, width: Float): String {
        val flat = text.replace('\n', ' ').trim()
        if (width <= 0f) return ""
        if (paint.measureText(flat) <= width) return flat
        val room = width - paint.measureText(ELLIPSIS)
        if (room <= 0f) return ""
        val count = paint.breakText(flat, true, room, null)
        return flat.substring(0, count).trimEnd() + ELLIPSIS
    }
}
