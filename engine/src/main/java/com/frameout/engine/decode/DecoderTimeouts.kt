package com.frameout.engine.decode

internal object DecoderTimeouts {
    private const val BASE_MS = 15_000L
    private const val PER_MEGAPIXEL_MS = 1_500L
    private const val MAX_MS = 90_000L

    fun outputMs(width: Int, height: Int): Long {
        val megapixels = width.toLong().coerceAtLeast(0L) * height.toLong().coerceAtLeast(0L) / 1_000_000L
        return (BASE_MS + megapixels * PER_MEGAPIXEL_MS).coerceAtMost(MAX_MS)
    }
}
