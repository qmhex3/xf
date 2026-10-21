package com.frameout.core.quality

object FrameIntegrity {
    enum class Finding { CountMismatch, TailShort, VariableRate, DuplicateTimestamps, SlowMotion }

    data class Input(
        val expected: Int?,
        val actual: Int,
        val ptsUs: List<Long>,
        val durationMs: Long,
        val rateCheck: Boolean,
        val tailCheck: Boolean,
        val stride: Int = 1,
        val captureFps: Double? = null
    )

    data class Report(
        val expected: Int?,
        val actual: Int,
        val measuredFps: Double?,
        val captureFps: Double?,
        val minGapMs: Double,
        val maxGapMs: Double,
        val tailMs: Long,
        val duplicateTimestamps: Int,
        val findings: List<Finding>
    ) {
        val healthy: Boolean get() = findings.isEmpty()
    }

    fun analyze(input: Input): Report {
        val stride = input.stride.coerceAtLeast(1)
        val gaps = input.ptsUs.zipWithNext { a, b -> b - a }
        val duplicates = gaps.count { it <= 0L }
        val positive = gaps.filter { it > 0L }.sorted()
        val median = positive.getOrNull(positive.size / 2)
        val minGap = positive.firstOrNull() ?: 0L
        val maxGap = positive.lastOrNull() ?: 0L
        val measured = if (input.rateCheck && median != null) 1_000_000.0 * stride / median else null
        val findings = ArrayList<Finding>()
        val expected = input.expected
        if (expected != null && expected != input.actual) findings += Finding.CountMismatch
        val tail = tailMillis(input, median)
        if (tail > 0L) findings += Finding.TailShort
        if (input.rateCheck && positive.size >= 3 && median != null && (maxGap > median * 3 / 2 || minGap < median / 2)) findings += Finding.VariableRate
        if (duplicates > 0) findings += Finding.DuplicateTimestamps
        val capture = input.captureFps
        if (measured != null && capture != null && measured > 0.0 && capture >= measured * 1.5) findings += Finding.SlowMotion
        return Report(expected, input.actual, measured, capture, minGap / 1000.0, maxGap / 1000.0, tail, duplicates, findings)
    }

    private fun tailMillis(input: Input, median: Long?): Long {
        if (!input.tailCheck || median == null || input.ptsUs.size < 2 || input.durationMs <= 0L) return 0L
        val last = input.ptsUs.last()
        val missingUs = input.durationMs * 1000L - last - median
        val threshold = maxOf(median * 2, 200_000L)
        return if (missingUs > threshold) missingUs / 1000L else 0L
    }
}
