package com.frameout.engine.audio

import android.content.Context
import android.net.Uri

object AudioMarkers {
    const val MAX_MARKERS = 12
    const val MIN_GAP_MS = 1500L

    suspend fun find(context: Context, uri: Uri): List<Long> {
        val levels = AudioLevelScanner(context).scan(uri)
        return PeakPicker.pick(levels, AudioLevelScanner.WINDOW_MS, MAX_MARKERS, MIN_GAP_MS)
    }
}
