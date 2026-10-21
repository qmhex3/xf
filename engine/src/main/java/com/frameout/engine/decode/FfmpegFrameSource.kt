package com.frameout.engine.decode

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.frameout.codecpack.FfmpegBridge

class FfmpegFrameSource(context: Context) : AnimatedFrameSource {
    private val router = DecodeRouter(context).also { it.forceFfmpeg = true; it.previewMaxSide = PREVIEW_SIDE }
    private var table = LongArray(0)
    private var count = 0

    override fun open(uri: Uri) {
        close()
        router.open(uri)
        val path = router.nativePath() ?: error("FFmpeg path missing")
        val raw = FfmpegBridge.nativeFrameIndex(path)
        check(raw != null && raw.isNotEmpty() && raw.size % STRIDE == 0) { "FFmpeg index failed" }
        FfmpegBridge.nativeSetSessionTable(path, raw)
        table = raw
        count = raw.size / STRIDE
    }

    override fun close() {
        router.close()
        table = LongArray(0)
        count = 0
    }

    override fun frameCount(): Int = count
    override fun videoWidth(): Int = router.ffmpegWidth()
    override fun videoHeight(): Int = router.ffmpegHeight()
    override fun ptsUs(index: Int): Long = if (count == 0) 0L else table[index.coerceIn(0, count - 1) * STRIDE]

    override fun durationUs(): Long {
        if (count == 0) return 0L
        val first = ptsUs(0)
        val last = ptsUs(count - 1)
        val gap = if (count > 1) (last - first) / (count - 1) else DEFAULT_GAP_US
        return last + gap.coerceAtLeast(1L)
    }

    override fun decodeFrame(index: Int): Pair<Long, Bitmap>? {
        if (count == 0) return null
        val safe = index.coerceIn(0, count - 1)
        val frame = if (router.wantsKeyframeHint(safe)) {
            router.decodeFfmpegFrame(safe, table[safe * STRIDE + 2].toInt(), table[safe * STRIDE + 3])
        } else {
            router.decodeFfmpegFrame(safe)
        }
        return frame?.let { table[safe * STRIDE] to it.bitmap }
    }

    private companion object {
        const val STRIDE = 4
        const val PREVIEW_SIDE = 1280
        const val DEFAULT_GAP_US = 33_333L
    }
}
