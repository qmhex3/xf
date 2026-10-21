package com.frameout.core.export

import java.io.ByteArrayOutputStream

object WebpFrameChunks {
    private val kept = setOf("VP8 ", "VP8L", "ALPH")

    fun extract(webp: ByteArray): ByteArray {
        require(webp.size > 20 && tag(webp, 0) == "RIFF" && tag(webp, 8) == "WEBP") { "Not a WebP file" }
        val out = ByteArrayOutputStream()
        var hasImage = false
        var at = 12
        while (at + 8 <= webp.size) {
            val name = tag(webp, at)
            val size = readLe32(webp, at + 4)
            val end = at + 8 + size
            require(size >= 0 && end <= webp.size) { "Truncated chunk" }
            if (name in kept) {
                out.write(webp, at, 8 + size)
                if (size and 1 == 1) out.write(0)
                hasImage = hasImage || name != "ALPH"
            }
            at = end + (size and 1)
        }
        require(hasImage) { "No image data" }
        return out.toByteArray()
    }

    fun dimensions(webp: ByteArray): Pair<Int, Int> {
        require(webp.size > 20 && tag(webp, 0) == "RIFF" && tag(webp, 8) == "WEBP") { "Not a WebP file" }
        var image: Pair<Int, Int>? = null
        var at = 12
        while (at + 8 <= webp.size) {
            val name = tag(webp, at)
            val size = readLe32(webp, at + 4)
            val end = at + 8 + size
            require(size >= 0 && end <= webp.size) { "Truncated chunk" }
            val body = at + 8
            when (name) {
                "VP8X" -> {
                    require(size >= 10) { "Bad VP8X chunk" }
                    return (readLe(webp, body + 4, 3) + 1) to (readLe(webp, body + 7, 3) + 1)
                }
                "VP8 " -> if (image == null) {
                    require(size >= 10 && (webp[body + 3].toInt() and 0xFF) == 0x9D && (webp[body + 4].toInt() and 0xFF) == 0x01 && (webp[body + 5].toInt() and 0xFF) == 0x2A) { "Bad VP8 header" }
                    image = (readLe(webp, body + 6, 2) and 0x3FFF) to (readLe(webp, body + 8, 2) and 0x3FFF)
                }
                "VP8L" -> if (image == null) {
                    require(size >= 5 && (webp[body].toInt() and 0xFF) == 0x2F) { "Bad VP8L header" }
                    val bits = readLe(webp, body + 1, 4)
                    image = ((bits and 0x3FFF) + 1) to (((bits shr 14) and 0x3FFF) + 1)
                }
            }
            at = end + (size and 1)
        }
        return requireNotNull(image) { "No image data" }
    }

    private fun readLe(bytes: ByteArray, at: Int, count: Int): Int {
        var value = 0
        for (i in 0 until count) value = value or ((bytes[at + i].toInt() and 0xFF) shl (8 * i))
        return value
    }

    private fun tag(bytes: ByteArray, at: Int): String = String(bytes, at, 4, Charsets.US_ASCII)

    private fun readLe32(bytes: ByteArray, at: Int): Int =
        (bytes[at].toInt() and 0xFF) or
            ((bytes[at + 1].toInt() and 0xFF) shl 8) or
            ((bytes[at + 2].toInt() and 0xFF) shl 16) or
            ((bytes[at + 3].toInt() and 0xFF) shl 24)
}
