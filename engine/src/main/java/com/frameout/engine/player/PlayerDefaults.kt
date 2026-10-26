package com.frameout.engine.player

import androidx.media3.exoplayer.DefaultLoadControl

internal const val NO_POSITION = Long.MIN_VALUE
internal const val END_TOLERANCE_MS = WindowRules.END_TOLERANCE_MS
internal const val MIN_WINDOW_MS = 100L

internal const val SPINNER_DELAY_MS = 900L

internal fun smoothLoadControl(): DefaultLoadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(15_000, 40_000, 250, 600)
    .setBackBuffer(15_000, true)
    .setPrioritizeTimeOverSizeThresholds(true)
    .build()
