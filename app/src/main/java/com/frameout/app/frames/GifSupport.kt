package com.frameout.app.frames

import com.frameout.core.db.FrameEntity
import com.frameout.core.export.ExportBitmaps
import com.frameout.core.export.GifEncoder
import java.io.OutputStream

internal object GifSupport {
    const val SIDE = 480
    const val FRAME_BYTES = 256L * 1024L

    fun delayMs(frames: List<FrameEntity>): Int {
        val gaps = frames.zipWithNext { a, b -> (b.ptsUs - a.ptsUs) / 1000L }.filter { it > 0 }.sorted()
        val median = gaps.getOrNull(gaps.size / 2) ?: 300L
        return median.coerceIn(80L, 1000L).toInt()
    }

    fun dimensions(paths: List<String?>): Pair<Int, Int> {
        val first = paths.firstNotNullOfOrNull { ExportBitmaps.load(it, SIDE) } ?: return 0 to 0
        val size = ExportBitmaps.fitSize(first.width, first.height, SIDE)
        first.recycle()
        return size
    }

    fun write(paths: List<String?>, delay: Int, out: OutputStream) {
        val size = dimensions(paths)
        val encoder = GifEncoder(out, size.first, size.second, delay)
        encoder.begin()
        paths.forEach { path ->
            val source = ExportBitmaps.load(path, SIDE) ?: return@forEach
            val frame = ExportBitmaps.letterbox(source, size.first, size.second)
            source.recycle()
            encoder.addFrame(frame)
            frame.recycle()
        }
        encoder.finish()
    }
}
