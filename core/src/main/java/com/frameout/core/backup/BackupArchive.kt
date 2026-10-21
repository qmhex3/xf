package com.frameout.core.backup

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupWriter(out: OutputStream) {
    private val zip = ZipOutputStream(out.buffered()).apply { setLevel(Deflater.BEST_SPEED) }

    fun manifest(data: BackupData) {
        val bytes = BackupManifest.encode(data).toByteArray(Charsets.UTF_8)
        if (bytes.size > BackupFormat.MAX_MANIFEST_BYTES) throw BackupException(BackupException.Reason.TooLarge)
        zip.putNextEntry(ZipEntry(BackupFormat.MANIFEST))
        zip.write(bytes)
        zip.closeEntry()
    }

    private var thumbBytes = 0L

    fun thumb(index: Int, file: File) {
        if (!file.isFile) return
        val size = file.length()
        if (size <= 0L || size > BackupFormat.MAX_THUMB_BYTES || thumbBytes + size > BackupFormat.MAX_THUMBS_TOTAL_BYTES) return
        thumbBytes += size
        zip.putNextEntry(ZipEntry(BackupFormat.thumbEntry(index)))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    fun finish() {
        zip.finish()
        zip.flush()
    }
}

class BackupReader(input: InputStream) : Closeable {
    private val zip = ZipInputStream(input.buffered())

    fun manifest(): BackupData {
        val entry = zip.nextEntry
        if (entry == null || entry.name != BackupFormat.MANIFEST) throw BackupException(BackupException.Reason.Invalid)
        val limited = LimitedStream(zip, BackupFormat.MAX_MANIFEST_BYTES)
        val reader = BufferedReader(InputStreamReader(limited, Charsets.UTF_8))
        return BackupManifest.decode(reader.lineSequence().iterator())
    }

    fun thumbs(maxTotalBytes: Long = BackupFormat.MAX_THUMBS_TOTAL_BYTES, target: (Int) -> File?): Set<Int> {
        val written = HashSet<Int>()
        var entries = 0
        var totalBytes = 0L
        while (true) {
            val entry = zip.nextEntry ?: return written
            if (++entries > BackupFormat.MAX_ENTRIES) throw BackupException(BackupException.Reason.TooLarge)
            val index = BackupFormat.thumbIndex(entry.name) ?: continue
            if (index in written) continue
            val file = target(index) ?: continue
            val size = try {
                file.outputStream().use { copyLimited(it, BackupFormat.MAX_THUMB_BYTES, maxTotalBytes - totalBytes) }
            } catch (e: BackupException) {
                file.delete()
                throw e
            }
            totalBytes += size
            written.add(index)
        }
    }

    private fun copyLimited(out: OutputStream, limit: Long, remaining: Long): Long {
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) return total
            total += read
            if (total > limit || total > remaining) throw BackupException(BackupException.Reason.TooLarge)
            out.write(buffer, 0, read)
        }
    }

    override fun close() = zip.close()
}

private class LimitedStream(private val source: InputStream, private val limit: Long) : InputStream() {
    private var total = 0L

    override fun read(): Int {
        val b = source.read()
        if (b >= 0 && ++total > limit) throw BackupException(BackupException.Reason.TooLarge)
        return b
    }

    override fun read(buffer: ByteArray, off: Int, len: Int): Int {
        val n = source.read(buffer, off, len)
        if (n > 0) {
            total += n
            if (total > limit) throw BackupException(BackupException.Reason.TooLarge)
        }
        return n
    }

    override fun close() {}
}
