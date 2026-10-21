package com.frameout.engine.audio

data class AudioSegment(val startUs: Long, val endUs: Long)

object AudioScanPlan {
    const val BUDGET_US = 600_000_000L
    const val SEGMENT_US = 30_000_000L

    fun segments(durationUs: Long, budgetUs: Long = BUDGET_US, segmentUs: Long = SEGMENT_US): List<AudioSegment> {
        if (durationUs <= 0L) return listOf(AudioSegment(0L, budgetUs))
        if (durationUs <= budgetUs) return listOf(AudioSegment(0L, durationUs))
        val count = (budgetUs / segmentUs).toInt().coerceAtLeast(1)
        if (count == 1) return listOf(AudioSegment(0L, segmentUs))
        val span = durationUs - segmentUs
        return List(count) {
            val start = it * span / (count - 1)
            AudioSegment(start, start + segmentUs)
        }
    }
}

class AudioScan(val levels: FloatArray, val complete: Boolean)
