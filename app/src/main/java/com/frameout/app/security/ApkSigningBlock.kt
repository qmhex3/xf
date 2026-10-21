package com.frameout.app.security

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

internal object ApkSigningBlock {
    private const val MAGIC = "APK Sig Block 42"
    private const val MAX_BLOCK = 32L * 1024 * 1024
    private val SIGNER_IDS = setOf(0x7109871aL, 0xf05368c0L, 0x1b93ad61L)

    fun signerDigests(apk: File): Set<String> = runCatching {
        RandomAccessFile(apk, "r").use { raf ->
            readPairs(raf).filterKeys { it in SIGNER_IDS }.values.flatMap { certDigests(it) }.toSet()
        }
    }.getOrDefault(emptySet())

    private fun centralDirectoryOffset(raf: RandomAccessFile): Long {
        val length = raf.length()
        val size = minOf(length, 65557L).toInt()
        val tail = ByteArray(size)
        raf.seek(length - size)
        raf.readFully(tail)
        for (i in size - 22 downTo 0) {
            val hit = tail[i] == 0x50.toByte() && tail[i + 1] == 0x4b.toByte() && tail[i + 2] == 5.toByte() && tail[i + 3] == 6.toByte()
            if (hit) return ByteBuffer.wrap(tail, i + 16, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffffffffL
        }
        error("no central directory")
    }

    private fun readPairs(raf: RandomAccessFile): Map<Long, ByteArray> {
        val cd = centralDirectoryOffset(raf)
        val footer = ByteArray(24)
        raf.seek(cd - 24)
        raf.readFully(footer)
        check(String(footer, 8, 16, Charsets.ISO_8859_1) == MAGIC)
        val size = ByteBuffer.wrap(footer, 0, 8).order(ByteOrder.LITTLE_ENDIAN).long
        check(size in 24L..MAX_BLOCK && size <= cd)
        val body = ByteArray((size - 24).toInt())
        raf.seek(cd - size)
        raf.readFully(body)
        return parsePairs(ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN))
    }

    private fun parsePairs(buf: ByteBuffer): Map<Long, ByteArray> {
        val out = HashMap<Long, ByteArray>()
        while (buf.remaining() >= 12) {
            val len = buf.long
            check(len in 4L..buf.remaining().toLong())
            val id = buf.int.toLong() and 0xffffffffL
            val value = ByteArray((len - 4).toInt())
            buf.get(value)
            out.putIfAbsent(id, value)
        }
        return out
    }

    private fun certDigests(value: ByteArray): List<String> {
        val signers = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).chunk()
        val result = ArrayList<String>()
        while (signers.hasRemaining()) {
            val signedData = signers.chunk().chunk()
            signedData.chunk()
            val certs = signedData.chunk()
            while (certs.hasRemaining()) result += sha256Hex(certs.chunk().bytes())
        }
        return result
    }

    private fun ByteBuffer.chunk(): ByteBuffer {
        val n = int
        require(n in 0..remaining())
        val view = duplicate().order(ByteOrder.LITTLE_ENDIAN)
        view.limit(position() + n)
        val out = view.slice().order(ByteOrder.LITTLE_ENDIAN)
        position(position() + n)
        return out
    }

    private fun ByteBuffer.bytes(): ByteArray = ByteArray(remaining()).also { get(it) }

    private fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
}
