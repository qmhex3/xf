package com.frameout.core.media

internal object LiveContentId {
    private const val QT_KEY = "com.apple.quicktime.content.identifier"
    private const val MAKER_TAG = 0x0011
    private const val MAKER_HEADER = 14
    private val uuid = Regex("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")

    fun parse(bytes: ByteArray, length: Int = bytes.size): String? {
        val n = length.coerceIn(0, bytes.size)
        return fromQuickTime(bytes, n) ?: fromMakerNote(bytes, n)
    }

    fun fromQuickTime(bytes: ByteArray, length: Int): String? {
        var from = 0
        while (true) {
            val at = indexOf(bytes, length, "keys", from)
            if (at < 4) return null
            from = at + 4
            val found = readKeysBox(bytes, length, at) ?: continue
            return found
        }
    }

    private fun readKeysBox(bytes: ByteArray, length: Int, typeAt: Int): String? {
        val boxStart = typeAt - 4
        val boxEnd = (boxStart.toLong() + u32(bytes, boxStart, length)).coerceAtMost(length.toLong()).toInt()
        if (boxEnd <= typeAt + 12) return null
        val count = u32(bytes, typeAt + 8, length).coerceAtMost(4096L).toInt()
        var p = typeAt + 12
        var index = -1
        for (i in 1..count) {
            if (p + 8 > boxEnd) return null
            val size = u32(bytes, p, length).toInt()
            if (size < 8 || p + size > boxEnd) return null
            if (String(bytes, p + 8, size - 8, Charsets.ISO_8859_1) == QT_KEY) index = i
            p += size
        }
        if (index < 0) return null
        val listAt = indexOf(bytes, length, "ilst", boxEnd)
        if (listAt < 4) return null
        val listEnd = (listAt - 4 + u32(bytes, listAt - 4, length)).coerceAtMost(length.toLong()).toInt()
        var q = listAt + 4
        while (q + 8 <= listEnd) {
            val itemSize = u32(bytes, q, length).toInt()
            if (itemSize < 16) return null
            if (u32(bytes, q + 4, length).toInt() == index) return readData(bytes, q + 8, minOf(q + itemSize, listEnd))
            q += itemSize
        }
        return null
    }

    private fun readData(bytes: ByteArray, at: Int, end: Int): String? {
        if (at + 16 > end) return null
        val size = u32(bytes, at, bytes.size).toInt()
        if (size < 16 || at + size > end) return null
        val text = String(bytes, at + 16, size - 16, Charsets.ISO_8859_1).trim { it <= ' ' }
        return uuid.find(text)?.value?.lowercase()
    }

    fun fromMakerNote(bytes: ByteArray, length: Int): String? {
        val start = indexOf(bytes, length, "Apple iOS", 0)
        if (start < 0) return null
        val viaIfd = readMakerIfd(bytes, length, start)
        if (viaIfd != null) return viaIfd
        val window = minOf(4096, length - start)
        return uuid.find(String(bytes, start, window, Charsets.ISO_8859_1))?.value?.lowercase()
    }

    private fun readMakerIfd(bytes: ByteArray, length: Int, start: Int): String? {
        val order = start + 12
        if (order + 2 > length) return null
        val big = bytes[order] == 'M'.code.toByte()
        if (!big && bytes[order] != 'I'.code.toByte()) return null
        val ifd = start + MAKER_HEADER
        if (ifd + 2 > length) return null
        val entries = u16(bytes, ifd, big).coerceAtMost(512)
        for (i in 0 until entries) {
            val e = ifd + 2 + i * 12
            if (e + 12 > length) return null
            if (u16(bytes, e, big) != MAKER_TAG) continue
            val count = u32(bytes, e + 4, length, big).coerceAtMost(256L).toInt()
            val offset = if (count <= 4) e + 8 else start + u32(bytes, e + 8, length, big).toInt()
            if (offset < 0 || offset + count > length) return null
            return uuid.find(String(bytes, offset, count, Charsets.ISO_8859_1))?.value?.lowercase()
        }
        return null
    }

    private fun u16(bytes: ByteArray, at: Int, big: Boolean): Int {
        val a = bytes[at].toInt() and 0xff
        val b = bytes[at + 1].toInt() and 0xff
        return if (big) (a shl 8) or b else (b shl 8) or a
    }

    private fun u32(bytes: ByteArray, at: Int, length: Int, big: Boolean = true): Long {
        if (at < 0 || at + 4 > length) return 0L
        var v = 0L
        for (k in 0 until 4) {
            val b = (bytes[if (big) at + k else at + 3 - k].toLong() and 0xff)
            v = (v shl 8) or b
        }
        return v
    }

    private fun indexOf(bytes: ByteArray, length: Int, needle: String, from: Int): Int {
        val pattern = needle.toByteArray(Charsets.ISO_8859_1)
        var i = maxOf(from, 0)
        val last = length - pattern.size
        while (i <= last) {
            var j = 0
            while (j < pattern.size && bytes[i + j] == pattern[j]) j++
            if (j == pattern.size) return i
            i++
        }
        return -1
    }
}
