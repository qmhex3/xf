package com.frameout.engine.decode

import java.nio.ByteBuffer

internal object Vp9Color {
    private const val MIN_SIZE = 12
    private const val UNSPECIFIED = 2

    fun parseVpcc(payload: ByteArray): Mp4Orientation.Colr? {
        if (payload.size < MIN_SIZE || payload[0].toInt() < 1) return null
        val full = (payload[6].toInt() and 1) == 1
        val primaries = payload[7].toInt() and 0xFF
        val transfer = payload[8].toInt() and 0xFF
        val matrix = payload[9].toInt() and 0xFF
        val described = primaries != UNSPECIFIED || transfer != UNSPECIFIED || matrix != UNSPECIFIED
        return Mp4Orientation.Colr(primaries, transfer, matrix, full, full || described)
    }

    fun parseVpcc(buffer: ByteBuffer, start: Int, end: Int): Mp4Orientation.Colr? =
        parseVpcc(ByteArray(end - start) { buffer.get(start + it) })
}
