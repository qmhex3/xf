package com.frameout.core.export
object ContactSheetLayout {
    const val SHEET_WIDTH = 2400
    const val MARGIN = 24
    const val GAP = 12
    const val HEADER = 72
    const val MIN_COLUMNS = 2
    const val MAX_COLUMNS = 8
    const val DEFAULT_COLUMNS = 4
    const val MAX_SHEET_PIXELS = 16_000_000L
    const val MAX_SHEETS = 12
    const val MIN_ASPECT = 0.4f
    const val MAX_ASPECT = 2.5f

    data class Options(
        val columns: Int = DEFAULT_COLUMNS,
        val showTime: Boolean = true,
        val showNumber: Boolean = true,
        val png: Boolean = false
    ) {
        val safeColumns: Int get() = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS)
    }

    data class Plan(
        val columns: Int,
        val cellW: Int,
        val cellH: Int,
        val captionH: Int,
        val textSize: Float,
        val rowsPerSheet: Int,
        val sheets: List<IntRange>,
        val count: Int
    ) {
        val sheetWidth: Int get() = SHEET_WIDTH
        val tooMany: Boolean get() = sheets.size > MAX_SHEETS

        fun sheetHeight(sheet: Int): Int {
            val range = sheets[sheet]
            val rows = (range.count() + columns - 1) / columns
            return MARGIN * 2 + HEADER + rows * (cellH + captionH) + (rows - 1).coerceAtLeast(0) * GAP
        }

        fun cellLeft(slot: Int): Int = MARGIN + (slot % columns) * (cellW + GAP)
        fun cellTop(slot: Int): Int = MARGIN + HEADER + (slot / columns) * (cellH + captionH + GAP)
    }

    fun cellWidth(columns: Int): Int = (SHEET_WIDTH - 2 * MARGIN - (columns - 1) * GAP) / columns

    fun plan(count: Int, aspect: Float, options: Options): Plan {
        val columns = options.safeColumns
        val cellW = cellWidth(columns)
        val safeAspect = if (aspect.isFinite() && aspect > 0f) aspect.coerceIn(MIN_ASPECT, MAX_ASPECT) else 16f / 9f
        val cellH = (cellW / safeAspect).toInt().coerceAtLeast(2)
        val captioned = options.showTime || options.showNumber
        val textSize = (cellW * 0.085f).coerceIn(16f, 44f)
        val captionH = if (captioned) (textSize * 1.7f).toInt() else 0
        val rowH = cellH + captionH + GAP
        val maxHeight = (MAX_SHEET_PIXELS / SHEET_WIDTH).toInt()
        val rows = ((maxHeight - 2 * MARGIN - HEADER + GAP) / rowH).coerceAtLeast(1)
        val perSheet = rows * columns
        val sheets = if (count <= 0) emptyList() else (0 until count step perSheet).map { it until minOf(count, it + perSheet) }
        return Plan(columns, cellW, cellH, captionH, textSize, rows, sheets, count)
    }

    fun fit(w: Int, h: Int, boxW: Int, boxH: Int): FloatArray {
        if (w <= 0 || h <= 0) return floatArrayOf(0f, 0f, boxW.toFloat(), boxH.toFloat())
        val scale = minOf(boxW.toFloat() / w, boxH.toFloat() / h)
        val dw = w * scale
        val dh = h * scale
        val left = (boxW - dw) / 2f
        val top = (boxH - dh) / 2f
        return floatArrayOf(left, top, left + dw, top + dh)
    }

    fun fileName(prefix: String, stamp: Long, sheet: Int, sheets: Int, png: Boolean): String {
        val suffix = if (sheets > 1) "_${sheet + 1}of$sheets" else ""
        return "${prefix}_contact_$stamp$suffix.${if (png) "png" else "jpg"}"
    }
}
