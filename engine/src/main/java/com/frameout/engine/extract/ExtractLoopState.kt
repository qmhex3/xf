package com.frameout.engine.extract

internal data class LoopState(var done: Int, var lastPts: Long, var histogram: FloatArray?, var position: Int, var thumbnailSet: Boolean, var lastSampleIndex: Int, var lastPulledPts: Long, var consecutiveFailures: Int = 0, var skipped: Int = 0, var sinceSpaceCheck: Int = 0, var driftMisses: Int = 0)
