package com.frameout.engine.index

import java.io.File
import kotlin.math.abs

internal object IndexReuse {
    const val DURATION_TOLERANCE_MS = 1000L
    private const val MARKER = "index.system"

    fun canReuse(markerCount: Int?, storedCount: Int, projectDurationMs: Long, trackDurationUs: Long): Boolean {
        if (markerCount == null || storedCount <= 0 || markerCount != storedCount) return false
        if (projectDurationMs <= 0L || trackDurationUs <= 0L) return false
        return abs(trackDurationUs / 1000L - projectDurationMs) <= DURATION_TOLERANCE_MS
    }

    fun readMarker(dir: File): Int? =
        runCatching { File(dir, MARKER).takeIf { it.isFile }?.readText()?.trim()?.toInt() }.getOrNull()

    fun writeMarker(dir: File, frameCount: Int) {
        runCatching { File(dir, MARKER).writeText(frameCount.toString()) }
    }

    fun clearMarker(dir: File) {
        File(dir, MARKER).delete()
    }
}
