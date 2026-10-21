package com.frameout.app.trim

data class TimelineZoom(val scale: Float = 1f, val start: Float = 0f) {
    val zoomed: Boolean get() = scale > MIN_SCALE
    val span: Float get() = 1f / scale
    val end: Float get() = start + span

    fun startMs(durationMs: Long): Float = start * durationMs
    fun spanMs(durationMs: Long): Float = span * durationMs

    fun transform(anchor: Float, pan: Float, zoom: Float, durationMs: Long): TimelineZoom {
        val next = (scale * zoom).coerceIn(MIN_SCALE, maxScaleFor(durationMs))
        if (next <= MIN_SCALE) return TimelineZoom()
        val a = anchor.coerceIn(0f, 1f)
        val under = start + a * span
        val nextSpan = 1f / next
        return TimelineZoom(next, clampStart(under - (a + pan) * nextSpan, nextSpan))
    }

    fun scrolledBy(fraction: Float): TimelineZoom {
        if (!zoomed) return this
        return copy(start = clampStart(start + fraction * span, span))
    }

    fun revealing(ms: Long, durationMs: Long): TimelineZoom {
        if (!zoomed || durationMs <= 0L) return this
        val at = ms.coerceIn(0L, durationMs).toFloat() / durationMs
        if (at >= start && at <= end) return this
        return copy(start = clampStart(at - span / 2f, span))
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 60f

        const val MIN_VISIBLE_MS = 1_000L

        fun maxScaleFor(durationMs: Long): Float =
            (durationMs.toFloat() / MIN_VISIBLE_MS).coerceIn(MIN_SCALE, MAX_SCALE)

        private fun clampStart(start: Float, span: Float): Float =
            start.coerceIn(0f, (1f - span).coerceAtLeast(0f))
    }
}
