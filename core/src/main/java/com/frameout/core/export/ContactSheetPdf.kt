package com.frameout.core.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object ContactSheetPdf {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 28f
    private const val HEADER = 36f
    private const val CAPTION_LINES = 2
    private const val CAPTION_SIZE = 9f
    private const val CAPTION_LEADING = 11f
    private const val CAPTION = CAPTION_LINES * CAPTION_LEADING + 4f
    private const val GAP = 10f
    private const val CELL_BITMAP_SIDE = 400
    private const val ELLIPSIS = "…"
    const val COLUMNS = 3
    const val ROWS = 4
    const val PER_PAGE = COLUMNS * ROWS

    fun write(paths: List<String?>, captions: List<String>, title: String, out: OutputStream) {
        require(paths.isNotEmpty()) { "No frames" }
        val document = PdfDocument()
        try {
            val pages = (paths.size + PER_PAGE - 1) / PER_PAGE
            for (page in 0 until pages) drawPage(document, page, pages, paths, captions, title)
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private fun drawPage(document: PdfDocument, page: Int, pages: Int, paths: List<String?>, captions: List<String>, title: String) {
        val started = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, page + 1).create())
        val canvas = started.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.color = 0xFF222222.toInt()
        paint.textSize = 14f
        val heading = fitLine("$title  ${page + 1}/$pages", paint, PAGE_W - 2 * MARGIN)
        canvas.drawText(heading, MARGIN, MARGIN + 14f, paint)
        val first = page * PER_PAGE
        for (slot in 0 until minOf(PER_PAGE, paths.size - first)) drawCell(canvas, slot, paths[first + slot], captions.getOrNull(first + slot), paint)
        document.finishPage(started)
    }

    private fun drawCell(canvas: Canvas, slot: Int, path: String?, caption: String?, paint: Paint) {
        val cellW = (PAGE_W - 2 * MARGIN - (COLUMNS - 1) * GAP) / COLUMNS
        val cellH = (PAGE_H - 2 * MARGIN - HEADER - (ROWS - 1) * GAP) / ROWS
        val left = MARGIN + (slot % COLUMNS) * (cellW + GAP)
        val top = MARGIN + HEADER + (slot / COLUMNS) * (cellH + GAP)
        val box = RectF(left, top, left + cellW, top + cellH - CAPTION)
        val bitmap = ExportBitmaps.load(path, CELL_BITMAP_SIDE)
        if (bitmap != null) {
            val scale = minOf(box.width() / bitmap.width, box.height() / bitmap.height)
            val w = bitmap.width * scale
            val h = bitmap.height * scale
            val dest = RectF(box.centerX() - w / 2f, box.centerY() - h / 2f, box.centerX() + w / 2f, box.centerY() + h / 2f)
            canvas.drawBitmap(bitmap, null, dest, paint)
            bitmap.recycle()
        }
        if (caption.isNullOrBlank()) return
        paint.color = 0xFF444444.toInt()
        paint.textSize = CAPTION_SIZE
        val cell = RectF(left, box.bottom, left + cellW, box.bottom + CAPTION)
        canvas.save()
        canvas.clipRect(cell)
        wrap(caption, paint, cellW, CAPTION_LINES).forEachIndexed { index, line ->
            canvas.drawText(line, left, box.bottom + 9f + index * CAPTION_LEADING, paint)
        }
        canvas.restore()
    }

    private fun fitLine(text: String, paint: Paint, width: Float): String {
        val flat = text.replace('\n', ' ').trim()
        if (paint.measureText(flat) <= width) return flat
        val room = width - paint.measureText(ELLIPSIS)
        if (room <= 0f) return ""
        val count = paint.breakText(flat, true, room, null)
        return flat.substring(0, count).trimEnd() + ELLIPSIS
    }

    private fun wrap(text: String, paint: Paint, width: Float, maxLines: Int): List<String> {
        val lines = ArrayList<String>(maxLines)
        var rest = text.replace('\n', ' ').trim()
        while (rest.isNotEmpty() && lines.size < maxLines) {
            if (lines.size == maxLines - 1) {
                lines.add(fitLine(rest, paint, width))
                break
            }
            if (paint.measureText(rest) <= width) {
                lines.add(rest)
                break
            }
            val count = paint.breakText(rest, true, width, null).coerceAtLeast(1)
            val space = rest.lastIndexOf(' ', count - 1)
            val cut = if (space > 0 && count < rest.length && rest[count] != ' ') space else count
            lines.add(rest.substring(0, cut).trimEnd())
            rest = rest.substring(cut).trimStart()
        }
        return lines
    }
}
