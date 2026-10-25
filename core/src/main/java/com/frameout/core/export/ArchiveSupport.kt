package com.frameout.core.export

import com.frameout.core.storage.AsciiName
import com.frameout.core.storage.FrameNaming
import java.io.OutputStream
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ArchiveNames {
    fun resolve(frames: List<StagedFrame>, reserved: Set<String>): List<StagedFrame> {
        val used = HashSet<String>()
        reserved.forEach { used.add(it.lowercase(Locale.ROOT)) }
        return frames.mapIndexed { position, frame ->
            val raw = frame.name.replace('/', '_').replace('\\', '_').ifBlank { "frame" }
            val safe = AsciiName.fileName(raw, "frame_${(position + 1).toString().padStart(4, '0')}")
            val name = FrameNaming.unique(safe, used)
            if (name == frame.name) frame else frame.withName(name)
        }
    }
}

object ZipSession {
    private val PRECOMPRESSED = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "avif")

    fun use(out: OutputStream, block: (ZipOutputStream) -> Unit) {
        val zip = ZipOutputStream(out.buffered())
        try {
            block(zip)
            zip.finish()
            zip.flush()
        } catch (e: Throwable) {
            runCatching { zip.close() }.onFailure { e.addSuppressed(it) }
            throw e
        }
        zip.close()
    }

    fun putBytes(zip: ZipOutputStream, name: String, bytes: ByteArray, time: Long? = null) {
        zip.putNextEntry(entry(name, time))
        zip.write(bytes)
        zip.closeEntry()
    }

    fun putFrame(zip: ZipOutputStream, frame: StagedFrame, time: Long? = null) {
        val entry = entry(frame.name, time)
        if (isPrecompressed(frame.name)) storeRaw(entry, frame.file)
        zip.putNextEntry(entry)
        frame.file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    fun isPrecompressed(name: String): Boolean = name.substringAfterLast('.', "").lowercase(Locale.ROOT) in PRECOMPRESSED

    private fun storeRaw(entry: ZipEntry, file: java.io.File) {
        val crc = CRC32()
        val buffer = ByteArray(64 * 1024)
        var size = 0L
        file.inputStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                crc.update(buffer, 0, read)
                size += read
            }
        }
        entry.method = ZipEntry.STORED
        entry.size = size
        entry.compressedSize = size
        entry.crc = crc.value
    }

    private fun entry(name: String, time: Long?): ZipEntry = ZipEntry(name).apply { if (time != null) this.time = time }
}
