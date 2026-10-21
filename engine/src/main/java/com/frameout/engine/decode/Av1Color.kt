package com.frameout.engine.decode

import android.media.MediaFormat

internal object Av1Color {
    private const val MIME = "video/av01"
    private const val OBU_SEQUENCE_HEADER = 1
    private const val AV1C_HEADER = 4
    private const val CP_BT709 = 1
    private const val TC_SRGB = 13
    private const val MC_IDENTITY = 0
    private const val UNSPECIFIED = 2

    fun read(format: MediaFormat): VuiColor.Signal? {
        val mime = if (format.containsKey(MediaFormat.KEY_MIME)) format.getString(MediaFormat.KEY_MIME) else null
        if (mime != MIME || !format.containsKey("csd-0")) return null
        val buffer = format.getByteBuffer("csd-0") ?: return null
        return parse(VuiColor.copy(buffer))?.takeIf { it.hasDescription || it.fullRange }
    }

    fun parse(data: ByteArray): VuiColor.Signal? {
        var p = if (data.isNotEmpty() && (data[0].toInt() and 0x80) != 0) AV1C_HEADER else 0
        while (p < data.size) {
            val header = data[p++].toInt() and 0xFF
            val type = (header shr 3) and 0xF
            if ((header and 4) != 0) p++
            var size = (data.size - p).toLong()
            if ((header and 2) != 0) {
                var value = 0L
                var shift = 0
                var more = true
                while (more) {
                    if (p >= data.size || shift >= 56) return null
                    val b = data[p++].toInt() and 0xFF
                    value = value or ((b and 0x7F).toLong() shl shift)
                    shift += 7
                    more = (b and 0x80) != 0
                }
                size = value
            }
            val end = minOf(p.toLong() + size, data.size.toLong()).toInt()
            if (type == OBU_SEQUENCE_HEADER) {
                return try {
                    sequenceHeader(VuiColor.Bits(data.copyOfRange(p, end)))
                } catch (_: VuiColor.Truncated) {
                    null
                }
            }
            p = end
        }
        return null
    }

    private fun sequenceHeader(r: VuiColor.Bits): VuiColor.Signal {
        val profile = r.u(3)
        r.skip(1)
        val reduced = r.flag()
        if (reduced) r.skip(5) else operatingPoints(r)
        val widthBits = r.u(4) + 1
        val heightBits = r.u(4) + 1
        r.skip(widthBits + heightBits)
        if (!reduced && r.flag()) r.skip(7)
        r.skip(3)
        if (!reduced) codingTools(r)
        r.skip(3)
        return colorConfig(r, profile)
    }

    private fun operatingPoints(r: VuiColor.Bits) {
        var decoderModel = false
        var bufferLength = 0
        if (r.flag()) {
            r.skip(64)
            if (r.flag()) r.uvlc()
            decoderModel = r.flag()
            if (decoderModel) {
                bufferLength = r.u(5) + 1
                r.skip(32)
                r.skip(10)
            }
        }
        val initialDelay = r.flag()
        repeat(r.u(5) + 1) {
            r.skip(12)
            if (r.u(5) > 7) r.skip(1)
            if (decoderModel && r.flag()) r.skip(bufferLength * 2 + 1)
            if (initialDelay && r.flag()) r.skip(4)
        }
    }

    private fun codingTools(r: VuiColor.Bits) {
        r.skip(4)
        val orderHint = r.flag()
        if (orderHint) r.skip(2)
        var screenContent = UNSPECIFIED
        if (!r.flag()) screenContent = r.u(1)
        if (screenContent > 0 && !r.flag()) r.skip(1)
        if (orderHint) r.skip(3)
    }

    private fun colorConfig(r: VuiColor.Bits, profile: Int): VuiColor.Signal {
        val highBitDepth = r.flag()
        if (profile == 2 && highBitDepth) r.skip(1)
        val mono = profile != 1 && r.flag()
        val described = r.flag()
        var primaries = UNSPECIFIED
        var transfer = UNSPECIFIED
        var matrix = UNSPECIFIED
        if (described) {
            primaries = r.u(8)
            transfer = r.u(8)
            matrix = r.u(8)
        }
        val srgb = primaries == CP_BT709 && transfer == TC_SRGB && matrix == MC_IDENTITY
        val full = if (!mono && srgb) true else r.flag()
        return VuiColor.Signal(full, described, primaries, transfer, matrix)
    }
}
