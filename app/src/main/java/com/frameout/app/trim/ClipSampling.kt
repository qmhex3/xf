package com.frameout.app.trim

internal data class ClipSamples(val timesMs: List<Long>, val frameMs: Int, val capped: Boolean)

internal object ClipSampling {
    const val SIDE = 480
    const val QUALITY = 70
    const val FPS = 12
    const val MAX_FRAMES = 120
    const val MAX_SPAN_MS = 20_000L
    private const val MIN_FRAMES = 2

    fun plan(startMs: Long, endMs: Long, maxSpanMs: Long = MAX_SPAN_MS, fps: Int = FPS, maxFrames: Int = MAX_FRAMES): ClipSamples? {
        val available = endMs - startMs
        if (available <= 0L) return null
        val span = minOf(available, maxSpanMs)
        val interval = maxOf(1000L / fps, ceilDiv(span, maxFrames.toLong()))
        val times = generateSequence(startMs) { it + interval }.takeWhile { it < startMs + span }.toList()
        if (times.size < MIN_FRAMES) return null
        return ClipSamples(times, interval.toInt(), available > maxSpanMs)
    }

    private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1L) / divisor
}
