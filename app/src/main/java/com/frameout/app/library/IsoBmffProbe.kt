package com.frameout.app.library

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
internal data class IsoVideoInfo(
    val width: Int,
    val height: Int,
    val sampleEntry: String,
    val transfer: Int?,
    val dolby: Boolean,
    val fps: Float?
)
internal object IsoBmffProbe {
    private const val MAX_MOOV = 32L * 1024 * 1024
    private fun cc(s: String) = s.fold(0) { acc, c -> (acc shl 8) or c.code }
    private val MOOV = cc("moov"); private val TRAK = cc("trak"); private val MDIA = cc("mdia")
    private val HDLR = cc("hdlr"); private val MDHD = cc("mdhd"); private val MINF = cc("minf")
    private val STBL = cc("stbl"); private val STSD = cc("stsd"); private val STTS = cc("stts")
    private val COLR = cc("colr"); private val NCLX = cc("nclx"); private val NCLC = cc("nclc"); private val VIDE = cc("vide")
    private val DOLBY_BOXES = setOf(cc("dvcC"), cc("dvvC"), cc("dvwC"))
    private val DOLBY_ENTRIES = setOf("dvh1", "dvhe", "dvav", "dva1", "dav1")

    fun probe(channel: FileChannel): IsoVideoInfo? {
        val size = channel.size()
        var pos = 0L
        val head = ByteBuffer.allocate(16)
        while (pos + 8 <= size) {
            head.clear()
            channel.read(head, pos)
            head.flip()
            if (head.remaining() < 8) return null
            var boxSize = head.int.toLong() and 0xFFFFFFFFL
            val type = head.int
            var header = 8
            if (boxSize == 1L) {
                if (head.remaining() < 8) return null
                boxSize = head.long
                header = 16
            } else if (boxSize == 0L) boxSize = size - pos
            if (boxSize < header) return null
            if (type == MOOV) {
                val payload = boxSize - header
                if (payload <= 0 || payload > MAX_MOOV || pos + boxSize > size) return null
                val buf = ByteBuffer.allocate(payload.toInt())
                var at = pos + header
                while (buf.hasRemaining()) {
                    val n = channel.read(buf, at)
                    if (n <= 0) return null
                    at += n
                }
                return parseMoov(buf)
            }
            pos += boxSize
        }
        return null
    }

    private inline fun boxes(b: ByteBuffer, from: Int, to: Int, visit: (type: Int, start: Int, end: Int) -> Boolean) {
        var p = from
        while (p + 8 <= to) {
            var size = b.getInt(p).toLong() and 0xFFFFFFFFL
            val type = b.getInt(p + 4)
            var header = 8
            if (size == 1L) { if (p + 16 > to) return; size = b.getLong(p + 8); header = 16 } else if (size == 0L) size = (to - p).toLong()
            if (size < header || p + size > to) return
            if (visit(type, p + header, (p + size).toInt())) return
            p += size.toInt()
        }
    }

    private fun parseMoov(b: ByteBuffer): IsoVideoInfo? {
        var result: IsoVideoInfo? = null
        boxes(b, 0, b.limit()) { type, s, e ->
            if (type == TRAK) result = parseTrak(b, s, e)
            result != null
        }
        return result
    }

    private fun parseTrak(b: ByteBuffer, s: Int, e: Int): IsoVideoInfo? {
        var out: IsoVideoInfo? = null
        boxes(b, s, e) { type, ms, me ->
            if (type == MDIA) out = parseMdia(b, ms, me)
            out != null
        }
        return out
    }

    private fun parseMdia(b: ByteBuffer, s: Int, e: Int): IsoVideoInfo? {
        var video = false
        var timescale = 0L
        var minf: IntArray? = null
        boxes(b, s, e) { type, ps, pe ->
            when (type) {
                HDLR -> if (ps + 12 <= pe) video = b.getInt(ps + 8) == VIDE
                MDHD -> if (ps + 24 <= pe) timescale = (if (b.get(ps).toInt() == 1) b.getInt(ps + 20) else b.getInt(ps + 12)).toLong() and 0xFFFFFFFFL
                MINF -> minf = intArrayOf(ps, pe)
            }
            false
        }
        val range = minf
        if (!video || range == null) return null
        var stbl: IntArray? = null
        boxes(b, range[0], range[1]) { type, ps, pe -> if (type == STBL) stbl = intArrayOf(ps, pe); stbl != null }
        val table = stbl ?: return null
        var entry: IsoVideoInfo? = null
        var fps: Float? = null
        boxes(b, table[0], table[1]) { type, ps, pe ->
            when (type) {
                STSD -> entry = parseStsd(b, ps, pe)
                STTS -> fps = parseStts(b, ps, pe, timescale)
            }
            false
        }
        return entry?.copy(fps = fps)
    }

    private fun parseStsd(b: ByteBuffer, s: Int, e: Int): IsoVideoInfo? {
        if (s + 8 + 8 + 78 > e || b.getInt(s + 4) < 1) return null
        val entry = s + 8
        val entrySize = (b.getInt(entry).toLong() and 0xFFFFFFFFL).toInt()
        val name = String(ByteArray(4) { b.get(entry + 4 + it) }, Charsets.ISO_8859_1)
        val width = b.getShort(entry + 8 + 24).toInt() and 0xFFFF
        val height = b.getShort(entry + 8 + 26).toInt() and 0xFFFF
        if (width == 0 || height == 0) return null
        var transfer: Int? = null
        var dolby = name.lowercase() in DOLBY_ENTRIES
        val end = minOf(entry + entrySize, e)
        boxes(b, entry + 8 + 78, end) { type, ps, pe ->
            if (type == COLR && ps + 10 <= pe) {
                val kind = b.getInt(ps)
                if (kind == NCLX || kind == NCLC) transfer = b.getShort(ps + 6).toInt() and 0xFFFF
            }
            if (type in DOLBY_BOXES) dolby = true
            false
        }
        return IsoVideoInfo(width, height, name, transfer, dolby, null)
    }

    private fun parseStts(b: ByteBuffer, s: Int, e: Int, timescale: Long): Float? {
        if (timescale <= 0 || s + 8 > e) return null
        val count = b.getInt(s + 4)
        if (count <= 0 || s + 8L + count * 8L > e) return null
        var samples = 0L
        var duration = 0L
        for (i in 0 until count) {
            val n = b.getInt(s + 8 + i * 8).toLong() and 0xFFFFFFFFL
            val d = b.getInt(s + 12 + i * 8).toLong() and 0xFFFFFFFFL
            samples += n
            duration += n * d
        }
        if (samples <= 0 || duration <= 0) return null
        return (samples.toDouble() * timescale / duration).toFloat()
    }
}
