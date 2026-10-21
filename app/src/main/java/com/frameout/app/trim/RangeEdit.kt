package com.frameout.app.trim

import com.frameout.engine.timeline.TrimRange

internal object RangeEdit {
    const val NUDGE_MS = 100L
    private const val MIN_SPAN_MS = 100L
    private val PRESET_SECONDS = listOf(1, 2, 3, 5, 10, 15, 30, 60)

    fun ceiling(durationMs: Long, maxMs: Long): Long = if (maxMs > 0L) minOf(maxMs, durationMs) else durationMs

    fun withLength(range: TrimRange, durationMs: Long, maxMs: Long, lengthMs: Long): TrimRange {
        val limit = ceiling(durationMs, maxMs)
        if (limit <= 0L) return range
        val length = lengthMs.coerceIn(minOf(MIN_SPAN_MS, limit), limit)
        val start = range.startMs.coerceIn(0L, (durationMs - length).coerceAtLeast(0L))
        return TrimRange(start, start + length, useFull = false)
    }

    fun presets(durationMs: Long, maxMs: Long): List<Long> {
        val limit = ceiling(durationMs, maxMs)
        val fixed = PRESET_SECONDS.map { it * 1000L }.filter { it <= limit }
        val cap = if (maxMs in 1L..durationMs && maxMs !in fixed) listOf(maxMs) else emptyList()
        return (fixed + cap).sorted()
    }

    fun matches(range: TrimRange, presetMs: Long): Boolean = !range.useFull && kotlin.math.abs(range.spanMs() - presetMs) < PRESET_TOLERANCE_MS

    private const val PRESET_TOLERANCE_MS = 50L
}

internal object TypedTime {
    const val MAX_DIGITS = 6

    fun digitsOf(text: String): String = text.filter { it in '0'..'9' }.trimStart('0').takeLast(MAX_DIGITS)

    fun appended(previous: String, next: String): String =
        if (next.length > previous.length && next.startsWith(previous)) next.substring(previous.length).filter { it in '0'..'9' } else ""

    fun label(digits: String): String {
        val padded = digits.padStart(4, '0')
        val seconds = padded.takeLast(2)
        val minutes = padded.dropLast(2).takeLast(2)
        val hours = padded.dropLast(4)
        return if (hours.isEmpty()) "$minutes:$seconds" else "$hours:$minutes:$seconds"
    }

    fun millis(digits: String): Long {
        val padded = digits.padStart(4, '0')
        val seconds = padded.takeLast(2).toLong()
        val minutes = padded.dropLast(2).takeLast(2).toLong()
        val hours = padded.dropLast(4).ifEmpty { "0" }.toLong()
        return ((hours * 60L + minutes) * 60L + seconds) * 1000L
    }
}
