package com.frameout.engine.timeline

data class TrimRange(
    val startMs: Long = 0,
    val endMs: Long = 0,
    val useFull: Boolean = true
) {
    fun clamped(durationMs: Long): TrimRange {
        if (durationMs <= 0) return copy(startMs = 0, endMs = 0)
        val s = startMs.coerceIn(0, durationMs)
        val e = endMs.coerceIn(s, durationMs).let { if (it <= s) durationMs else it }
        return copy(startMs = s, endMs = e)
    }

    fun spanMs(): Long = (endMs - startMs).coerceAtLeast(0)

    companion object {
        fun full(durationMs: Long) = TrimRange(0, durationMs, useFull = true)
    }
}

fun formatTimestamp(ms: Long): String {
    val total = ms.coerceAtLeast(0)
    val h = total / 3_600_000
    val m = (total % 3_600_000) / 60_000
    val s = (total % 60_000) / 1000
    val milli = total % 1000
    return if (h > 0) {
        "%d:%02d:%02d.%03d".format(java.util.Locale.US, h, m, s, milli)
    } else {
        "%02d:%02d.%03d".format(java.util.Locale.US, m, s, milli)
    }
}

fun parseTimestamp(text: String): Long? {
    val parts = text.trim().split(":")
    if (parts.size !in 1..3) return null
    val tail = parts.last().split(".")
    if (tail.size > 2) return null
    val whole = parts.dropLast(1) + tail[0]
    val fraction = tail.getOrNull(1).orEmpty()
    if (whole.any { !it.isAllDigits() } || (fraction.isNotEmpty() && !fraction.isAllDigits())) return null
    return try {
        val seconds = whole.fold(0L) { total, part -> Math.addExact(Math.multiplyExact(total, 60L), part.toLong()) }
        val millis = fraction.padEnd(3, '0').take(3).toLong()
        Math.addExact(Math.multiplyExact(seconds, 1000L), millis)
    } catch (_: ArithmeticException) {
        null
    } catch (_: NumberFormatException) {
        null
    }
}

private fun String.isAllDigits(): Boolean = isNotEmpty() && all { it.isDigit() }
