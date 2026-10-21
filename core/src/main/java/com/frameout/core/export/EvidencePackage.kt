package com.frameout.core.export

import com.frameout.core.storage.ExifText
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EvidenceSource(val name: String, val sha256: String?, val bytes: Long)

class EvidenceFrame(val staged: StagedFrame, val sha256: String)

object EvidencePackage {
    const val REPORT_NAME = "evidence_report.txt"
    const val JSON_NAME = "evidence.json"
    const val SUMS_NAME = "SHA256SUMS"
    const val ALGORITHM = "SHA-256"

    fun sha256(stream: InputStream): Pair<String, Long> {
        val digest = MessageDigest.getInstance(ALGORITHM)
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
            total += read
        }
        return hex(digest.digest()) to total
    }

    fun sha256(file: File): String = file.inputStream().use { sha256(it).first }

    fun sha256(bytes: ByteArray): String = hex(MessageDigest.getInstance(ALGORITHM).digest(bytes))

    fun hex(bytes: ByteArray): String {
        val chars = CharArray(bytes.size * 2)
        bytes.forEachIndexed { i, b ->
            val v = b.toInt() and 0xFF
            chars[i * 2] = HEX[v ushr 4]
            chars[i * 2 + 1] = HEX[v and 0x0F]
        }
        return String(chars)
    }

    fun utc(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))

    fun json(project: String, source: EvidenceSource, frames: List<EvidenceFrame>, createdAt: Long, appVersion: String): String {
        val items = JSONArray()
        frames.forEach { f ->
            items.put(
                JSONObject()
                    .put("sequence", f.staged.sequence)
                    .put("file", f.staged.name)
                    .put("frame_index", f.staged.frameIndex)
                    .put("time_ms", f.staged.ptsUs / 1000L)
                    .put("timecode", ExifText.position(f.staged.ptsUs))
                    .put("width", f.staged.width)
                    .put("height", f.staged.height)
                    .put("bytes", f.staged.file.length())
                    .put("sha256", f.sha256)
            )
        }
        val src = JSONObject().put("name", source.name).put("bytes", source.bytes).put("sha256", source.sha256 ?: JSONObject.NULL)
        return JSONObject()
            .put("tool", "Frame out $appVersion")
            .put("created_utc", utc(createdAt))
            .put("hash_algorithm", ALGORITHM)
            .put("project", project)
            .put("source", src)
            .put("count", frames.size)
            .put("frames", items)
            .toString(2)
    }

    fun report(project: String, source: EvidenceSource, frames: List<EvidenceFrame>, createdAt: Long, appVersion: String): String {
        val b = StringBuilder()
        b.append("FRAME EVIDENCE REPORT\r\n")
        b.append("=====================\r\n")
        b.append("Tool:        Frame out ").append(appVersion).append("\r\n")
        b.append("Created:     ").append(utc(createdAt)).append(" (UTC)\r\n")
        b.append("Algorithm:   ").append(ALGORITHM).append("\r\n")
        b.append("Project:     ").append(project).append("\r\n")
        b.append("Source:      ").append(source.name).append("\r\n")
        b.append("Source size: ").append(source.bytes).append(" bytes\r\n")
        b.append("Source hash: ").append(source.sha256 ?: "unavailable").append("\r\n")
        b.append("Frames:      ").append(frames.size).append("\r\n\r\n")
        frames.forEach { f ->
            b.append('#').append(f.staged.sequence).append("  ")
                .append(f.staged.name).append("  ")
                .append(ExifText.position(f.staged.ptsUs)).append("  ")
                .append(f.staged.width).append('x').append(f.staged.height).append("  ")
                .append(f.staged.file.length()).append(" bytes\r\n")
                .append("    ").append(f.sha256).append("\r\n")
        }
        b.append("\r\nVerify: run sha256sum -c ").append(SUMS_NAME).append(" after extracting the archive.\r\n")
        return b.toString()
    }

    fun sums(frames: List<EvidenceFrame>, extra: Map<String, String>): String {
        val b = StringBuilder()
        frames.forEach { b.append(it.sha256).append("  ").append(it.staged.name).append('\n') }
        extra.forEach { (name, hash) -> b.append(hash).append("  ").append(name).append('\n') }
        return b.toString()
    }

    fun write(frames: List<EvidenceFrame>, project: String, source: EvidenceSource, createdAt: Long, appVersion: String, out: OutputStream) {
        val resolved = ArchiveNames.resolve(frames.map { it.staged }, setOf(REPORT_NAME, JSON_NAME, SUMS_NAME))
        val entries = frames.mapIndexed { i, f -> EvidenceFrame(resolved[i], f.sha256) }
        val reportBytes = report(project, source, entries, createdAt, appVersion).toByteArray(Charsets.UTF_8)
        val jsonBytes = json(project, source, entries, createdAt, appVersion).toByteArray(Charsets.UTF_8)
        val sumsBytes = sums(entries, linkedMapOf(REPORT_NAME to sha256(reportBytes), JSON_NAME to sha256(jsonBytes))).toByteArray(Charsets.UTF_8)
        ZipSession.use(out) { zip ->
            ZipSession.putBytes(zip, REPORT_NAME, reportBytes, createdAt)
            ZipSession.putBytes(zip, JSON_NAME, jsonBytes, createdAt)
            ZipSession.putBytes(zip, SUMS_NAME, sumsBytes, createdAt)
            entries.forEach { ZipSession.putFrame(zip, it.staged, createdAt) }
        }
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
