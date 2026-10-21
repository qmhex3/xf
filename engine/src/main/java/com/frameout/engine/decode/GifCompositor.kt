package com.frameout.engine.decode

internal fun interlacedRows(height: Int): IntArray {
    val rows = IntArray(height)
    var count = 0
    for ((start, step) in listOf(0 to 8, 4 to 8, 2 to 4, 1 to 2)) {
        var row = start
        while (row < height) {
            rows[count++] = row
            row += step
        }
    }
    return rows
}

internal class GifCompositor(private val index: GifIndex, private val read: (Long, Int) -> ByteArray) {
    private val canvas = IntArray(index.width * index.height)
    private val resetPoints = index.resetPoints()
    private var snapshot: IntArray? = null
    private var rendered = -1
    private var base = 0

    fun render(frame: Int): IntArray {
        val target = frame.coerceIn(0, index.frames.lastIndex)
        val start = ResetPoints.lastAtOrBefore(resetPoints, target)
        if (target < rendered || start > rendered) reset(start)
        while (rendered < target) advance()
        return canvas
    }

    private fun reset(start: Int) {
        canvas.fill(0)
        rendered = start - 1
        base = start
    }

    private fun advance() {
        val next = rendered + 1
        if (rendered >= base) dispose(index.frames[rendered])
        val frame = index.frames[next]
        if (frame.dispose == GifIndex.DISPOSE_PREVIOUS) saveSnapshot()
        draw(frame)
        rendered = next
    }

    private fun dispose(frame: GifFrameInfo) {
        when (frame.dispose) {
            GifIndex.DISPOSE_BACKGROUND -> clearRegion(frame)
            GifIndex.DISPOSE_PREVIOUS -> {
                val saved = snapshot
                if (saved != null) saved.copyInto(canvas) else clearRegion(frame)
            }
        }
    }

    private fun saveSnapshot() {
        val target = snapshot ?: IntArray(canvas.size).also { snapshot = it }
        canvas.copyInto(target)
    }

    private fun clearRegion(frame: GifFrameInfo) {
        val right = minOf(frame.x + frame.width, index.width)
        val bottom = minOf(frame.y + frame.height, index.height)
        if (right <= frame.x) return
        for (row in frame.y until bottom) canvas.fill(0, row * index.width + frame.x, row * index.width + right)
    }

    private fun draw(frame: GifFrameInfo) {
        val count = frame.width * frame.height
        val palette = frame.palette ?: index.globalPalette
        if (count <= 0 || palette == null) return
        val indices = GifLzw.decode(read(frame.dataOffset, frame.dataLength), count)
        val rows = if (frame.interlaced) interlacedRows(frame.height) else null
        for (row in 0 until frame.height) drawRow(frame, indices, palette, row, frame.y + (rows?.get(row) ?: row))
    }

    private fun drawRow(frame: GifFrameInfo, indices: ByteArray, palette: ByteArray, row: Int, targetRow: Int) {
        if (targetRow < 0 || targetRow >= index.height) return
        val colors = palette.size / 3
        val limit = minOf(frame.width, index.width - frame.x)
        for (col in 0 until limit) {
            val value = indices[row * frame.width + col].toInt() and 0xFF
            if (value == frame.transparent || value >= colors) continue
            val at = value * 3
            canvas[targetRow * index.width + frame.x + col] = OPAQUE or
                ((palette[at].toInt() and 0xFF) shl 16) or
                ((palette[at + 1].toInt() and 0xFF) shl 8) or
                (palette[at + 2].toInt() and 0xFF)
        }
    }

    private companion object {
        const val OPAQUE = -0x1000000
    }
}
