package com.frameout.app.trim

import com.frameout.engine.timeline.TrimRange

internal enum class TrimOutput(val maxMs: Long) {
    Frames(0L),
    Video(0L),
    Live(3_000L),
    Gif(8_000L),
    Webp(20_000L);

    val saveKind: com.frameout.designsystem.icons.SaveKind?
        get() = when (this) {
            Frames -> null
            Video -> com.frameout.designsystem.icons.SaveKind.Video
            Live, Gif, Webp -> com.frameout.designsystem.icons.SaveKind.Image
        }
    val limited: Boolean get() = maxMs > 0L
    val seconds: Int get() = (maxMs / 1000L).toInt()

    companion object {
        fun fromKey(key: String): TrimOutput = entries.firstOrNull { outputKind(it) == key } ?: Frames
    }
}

internal object TrimLimits {
    private const val MIN_SPAN_MS = 100L

    fun enforce(old: TrimRange, next: TrimRange, maxMs: Long): TrimRange {
        if (maxMs <= 0L || next.spanMs() <= maxMs) return next
        return if (next.startMs != old.startMs) next.copy(endMs = next.startMs + maxMs) else next.copy(startMs = next.endMs - maxMs)
    }

    fun fit(range: TrimRange, maxMs: Long): TrimRange {
        if (maxMs <= 0L || range.spanMs() <= maxMs) return range
        return range.copy(endMs = range.startMs + maxMs, useFull = false)
    }

    fun fitAt(range: TrimRange, maxMs: Long, positionMs: Long): TrimRange {
        if (maxMs <= 0L || range.spanMs() <= maxMs) return range
        val start = positionMs.coerceIn(range.startMs, range.endMs - maxMs)
        return range.copy(startMs = start, endMs = start + maxMs, useFull = false)
    }

    fun initialSelection(positionMs: Long, durationMs: Long): TrimRange {
        val start = if (durationMs - positionMs < 1_000L) (durationMs - 1_000L).coerceAtLeast(0L) else positionMs
        return TrimRange(start, durationMs, useFull = false)
    }

    fun moveStart(range: TrimRange, ms: Long, durationMs: Long, maxMs: Long): TrimRange {
        val start = ms.coerceIn(0L, (durationMs - MIN_SPAN_MS).coerceAtLeast(0L))
        val reach = if (maxMs > 0L) maxMs else durationMs
        val end = if (start >= range.endMs - MIN_SPAN_MS) minOf(durationMs, start + reach) else range.endMs
        return enforce(range, TrimRange(start, end, useFull = false), maxMs)
    }

    fun moveEnd(range: TrimRange, ms: Long, durationMs: Long, maxMs: Long): TrimRange {
        val end = ms.coerceIn(MIN_SPAN_MS, durationMs.coerceAtLeast(MIN_SPAN_MS))
        val reach = if (maxMs > 0L) maxMs else durationMs
        val start = if (end <= range.startMs + MIN_SPAN_MS) (end - reach).coerceAtLeast(0L) else range.startMs
        return enforce(range, TrimRange(start, end, useFull = false), maxMs)
    }
}
