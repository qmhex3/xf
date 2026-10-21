package com.frameout.core.export

import com.frameout.core.util.SmoothScale
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object SlidesPdf {
    private const val PAGE_W = 960
    private const val MAX_SIDE = 1280
    private const val MIN_PAGE_H = 240
    private const val MAX_PAGE_H = 3840
    private const val RENDER_SCALE = 1.5f
    private const val DEFAULT_PAGE_H = 540

    fun write(paths: List<String?>, out: OutputStream) {
        val document = PdfDocument()
        try {
            var number = 1
            var lastH = DEFAULT_PAGE_H
            var drawn = 0
            paths.forEach { path ->
                val bitmap = ExportBitmaps.load(path, MAX_SIDE)
                if (bitmap == null) {
                    drawPlaceholder(document, number++, lastH)
                    return@forEach
                }
                try {
                    val pageH = pageHeight(bitmap)
                    lastH = pageH
                    val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, pageH, number++).create())
                    drawFit(page.canvas, bitmap, pageH)
                    document.finishPage(page)
                    drawn++
                } finally {
                    bitmap.recycle()
                }
            }
            check(drawn > 0) { "No slides could be loaded" }
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private fun pageHeight(bitmap: Bitmap): Int =
        (PAGE_W.toFloat() * bitmap.height / bitmap.width).toInt().coerceIn(MIN_PAGE_H, MAX_PAGE_H)

    private fun drawPlaceholder(document: PdfDocument, number: Int, pageH: Int) {
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, pageH, number).create())
        val canvas = page.canvas
        canvas.drawColor(0xFFFFFFFF.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xFFBBBBBB.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        val inset = 48f
        canvas.drawRect(inset, inset, PAGE_W - inset, pageH - inset, paint)
        canvas.drawLine(inset, inset, PAGE_W - inset, pageH - inset, paint)
        canvas.drawLine(PAGE_W - inset, inset, inset, pageH - inset, paint)
        document.finishPage(page)
    }

    private fun drawFit(canvas: Canvas, bitmap: Bitmap, pageH: Int) {
        canvas.drawColor(0xFFFFFFFF.toInt())
        val scale = minOf(PAGE_W.toFloat() / bitmap.width, pageH.toFloat() / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        val dest = RectF((PAGE_W - w) / 2f, (pageH - h) / 2f, (PAGE_W + w) / 2f, (pageH + h) / 2f)
        val targetW = (w * RENDER_SCALE).toInt().coerceAtLeast(1)
        val targetH = (h * RENDER_SCALE).toInt().coerceAtLeast(1)
        val source = if (targetW < bitmap.width && targetH < bitmap.height) SmoothScale.scaled(bitmap, targetW, targetH, true) else bitmap
        canvas.drawBitmap(source, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        if (source !== bitmap) source.recycle()
    }
}
