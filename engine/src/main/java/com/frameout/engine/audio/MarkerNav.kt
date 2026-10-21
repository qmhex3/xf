package com.frameout.engine.audio

object MarkerNav {
    const val MARGIN_MS = 300L

    fun next(markers: List<Long>, positionMs: Long): Long? = markers.firstOrNull { it > positionMs + MARGIN_MS }

    fun previous(markers: List<Long>, positionMs: Long): Long? = markers.lastOrNull { it < positionMs - MARGIN_MS }
}
