package com.frameout.engine.decode

import android.content.Context
import android.net.Uri
import android.system.Os
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.atan2

internal object Mp4Orientation {
    class Colr(val primaries: Int, val transfer: Int, val matrix: Int, val full: Boolean, val rangeKnown: Boolean = true)

    class Result(val rotation: Int, val flipH: Boolean, val colr: Colr? = null)

    private const val MAX_MOOV = 64L * 1024 * 1024

    fun read(context: Context, uri: Uri, videoOrdinal: Int): Result? = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val moov = loadMoov(pfd.fileDescriptor, Os.fstat(pfd.fileDescriptor).st_size) ?: return@use null
            find(moov, videoOrdinal)
        }
    }.getOrNull()

    fun fromMatrix(a: Int, b: Int, c: Int, d: Int): Result {
        val det = a.toLong() * d - b.toLong() * c
        val flip = det < 0
        val clockwise = if (flip) Math.toDegrees(atan2(-b.toDouble(), -a.toDouble())) else Math.toDegrees(atan2(b.toDouble(), a.toDouble()))
        val normalized = ((Math.round(clockwise).toInt() % 360) + 360) % 360
        return Result((normalized + 45) / 90 % 4 * 90, flip)
    }

    internal fun pread(fd: java.io.FileDescriptor, offset: Long, length: Int): ByteArray? {
        val buffer = ByteArray(length)
        var done = 0
        while (done < length) {
            val n = Os.pread(fd, buffer, done, length - done, offset + done)
            if (n <= 0) return null
            done += n
        }
        return buffer
    }

    private fun loadMoov(fd: java.io.FileDescriptor, size: Long): ByteBuffer? {
        var offset = 0L
        while (offset + 8 <= size) {
            val head = pread(fd, offset, if (offset + 16 <= size) 16 else 8) ?: return null
            val buf = ByteBuffer.wrap(head).order(ByteOrder.BIG_ENDIAN)
            var boxSize = buf.getInt(0).toLong() and 0xFFFFFFFFL
            val type = buf.getInt(4)
            var headerLen = 8
            if (boxSize == 1L && head.size >= 16) {
                boxSize = buf.getLong(8)
                headerLen = 16
            } else if (boxSize == 0L) {
                boxSize = size - offset
            }
            if (boxSize < headerLen) return null
            if (type == TYPE_MOOV) {
                val payload = boxSize - headerLen
                if (payload <= 0 || payload > MAX_MOOV) return null
                val data = pread(fd, offset + headerLen, payload.toInt()) ?: return null
                return ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            }
            offset += boxSize
        }
        return null
    }

    private fun children(buf: ByteBuffer, start: Int, end: Int, visit: (type: Int, payload: Int, payloadEnd: Int) -> Boolean) {
        var p = start
        while (p + 8 <= end) {
            var size = buf.getInt(p).toLong() and 0xFFFFFFFFL
            val type = buf.getInt(p + 4)
            var header = 8
            if (size == 1L) {
                if (p + 16 > end) return
                size = buf.getLong(p + 8)
                header = 16
            } else if (size == 0L) {
                size = (end - p).toLong()
            }
            if (size < header || p + size > end) return
            if (visit(type, p + header, (p + size).toInt())) return
            p += size.toInt()
        }
    }

    private fun find(moov: ByteBuffer, ordinal: Int): Result? {
        var seen = 0
        var result: Result? = null
        children(moov, 0, moov.limit()) { type, payload, payloadEnd ->
            if (type != TYPE_TRAK) return@children false
            var matrix: IntArray? = null
            var video = false
            var colr: Colr? = null
            children(moov, payload, payloadEnd) { t, p, e ->
                if (t == TYPE_TKHD) matrix = matrixOf(moov, p, e)
                if (t == TYPE_MDIA) children(moov, p, e) { t2, p2, e2 ->
                    if (t2 == TYPE_HDLR && p2 + 12 <= e2 && moov.getInt(p2 + 8) == HANDLER_VIDE) video = true
                    if (t2 == TYPE_MINF) colr = colrOf(moov, p2, e2)
                    false
                }
                false
            }
            if (!video) return@children false
            if (seen == ordinal) {
                val m = matrix
                val base = if (m != null) fromMatrix(m[0], m[1], m[3], m[4]) else Result(0, false)
                result = Result(base.rotation, base.flipH, colr)
                return@children true
            }
            seen++
            false
        }
        return result
    }

    private fun inside(buf: ByteBuffer, start: Int, end: Int, wanted: Int): IntArray? {
        var found: IntArray? = null
        children(buf, start, end) { type, payload, payloadEnd ->
            if (type != wanted) return@children false
            found = intArrayOf(payload, payloadEnd)
            true
        }
        return found
    }

    private fun colrOf(buf: ByteBuffer, minfStart: Int, minfEnd: Int): Colr? {
        val stbl = inside(buf, minfStart, minfEnd, TYPE_STBL) ?: return null
        val stsd = inside(buf, stbl[0], stbl[1], TYPE_STSD) ?: return null
        val entry = stsd[0] + 8
        if (entry + 8 > stsd[1]) return null
        val entrySize = buf.getInt(entry).toLong() and 0xFFFFFFFFL
        val entryEnd = minOf(stsd[1].toLong(), entry + entrySize).toInt()
        val start = entry + 8 + VISUAL_ENTRY_FIELDS
        if (start + 8 > entryEnd) return null
        val colr = inside(buf, start, entryEnd, TYPE_COLR)?.let { colrBox(buf, it) }
        val vpcc = inside(buf, start, entryEnd, TYPE_VPCC)?.let { Vp9Color.parseVpcc(buf, it[0], it[1]) }
        return when {
            colr == null -> vpcc
            vpcc == null || colr.rangeKnown -> colr
            else -> Colr(colr.primaries, colr.transfer, colr.matrix, vpcc.full, vpcc.rangeKnown)
        }
    }

    private fun colrBox(buf: ByteBuffer, box: IntArray): Colr? {
        val kind = if (box[0] + 4 <= box[1]) buf.getInt(box[0]) else return null
        val nclx = kind == TYPE_NCLX
        if (!nclx && kind != TYPE_NCLC) return null
        if (box[0] + (if (nclx) 11 else 10) > box[1]) return null
        return Colr(
            buf.getShort(box[0] + 4).toInt() and 0xFFFF,
            buf.getShort(box[0] + 6).toInt() and 0xFFFF,
            buf.getShort(box[0] + 8).toInt() and 0xFFFF,
            nclx && (buf.get(box[0] + 10).toInt() and 0x80) != 0,
            nclx
        )
    }

    private fun matrixOf(buf: ByteBuffer, payload: Int, end: Int): IntArray? {
        if (payload + 4 > end) return null
        val version = buf.get(payload).toInt() and 0xFF
        val offset = payload + if (version == 1) 52 else 40
        if (offset + 36 > end) return null
        return IntArray(9) { buf.getInt(offset + it * 4) }
    }

    private fun tag(s: String): Int = (s[0].code shl 24) or (s[1].code shl 16) or (s[2].code shl 8) or s[3].code

    private val TYPE_MOOV = tag("moov")
    private val TYPE_TRAK = tag("trak")
    private val TYPE_TKHD = tag("tkhd")
    private val TYPE_MDIA = tag("mdia")
    private val TYPE_HDLR = tag("hdlr")
    private val TYPE_MINF = tag("minf")
    private val TYPE_STBL = tag("stbl")
    private val TYPE_STSD = tag("stsd")
    private val TYPE_COLR = tag("colr")
    private val TYPE_VPCC = tag("vpcC")
    private val TYPE_NCLX = tag("nclx")
    private val TYPE_NCLC = tag("nclc")
    private val HANDLER_VIDE = tag("vide")
    private const val VISUAL_ENTRY_FIELDS = 78
}
