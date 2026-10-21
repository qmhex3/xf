package com.frameout.engine.player

internal object WindowRules {
    const val END_TOLERANCE_MS = 50L
    const val LEAD_MS = 250L
    const val SETTLE_MS = 400L
    const val DRIFT_MS = 40L

    fun isOutside(start: Long, end: Long, position: Long): Boolean =
        position >= end - END_TOLERANCE_MS || position < start - LEAD_MS

    fun settleAfterPause(start: Long, end: Long, position: Long): Long? = when {
        position > end && position - end <= SETTLE_MS -> end
        position < start && start - position <= SETTLE_MS -> start
        else -> null
    }

    fun repairAfterRangeChange(start: Long, end: Long, position: Long): Long? =
        if (position < start - DRIFT_MS || position > end + DRIFT_MS) start else null
}
