package com.frameout.core.storage

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.math.abs

object PngMetadata {
    private val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private const val ihdrEnd = 8 + 4 + 4 + 13 + 4
    private const val ASCII = 2
    private const val BYTE = 1
    private const val LONG = 4
    private const val RATIONAL = 5
    private class Entry(val tag: Int, val type: Int, val count: Int, val data: ByteArray)
    private data class ChunkSpan(val type: String, val start: Int, val end: Int, val dataStart: Int, val dataEnd: Int)

    fun isPng(bytes: ByteArray): Boolean = parseChunks(bytes)?.isNotEmpty() == true

    fun chunks(takenAt: Long, description: String, location: GeoPoint?): ByteArray {
        val extra = ByteArrayOutputStream()
        extra.write(chunk("eXIf", exif(takenAt, description, location)))
        extra.write(chunk("tEXt", "Software".toByteArray(Charsets.ISO_8859_1) + 0 + "Frame out".toByteArray(Charsets.ISO_8859_1)))
        if (description.isNotBlank()) extra.write(chunk("iTXt", international("Description", description)))
        return extra.toByteArray()
    }

    fun embed(png: ByteArray, takenAt: Long, description: String, location: GeoPoint?): ByteArray {
        val parsed = parseChunks(png) ?: return png
        val metadata = chunks(takenAt, description, location)
        val out = ByteArrayOutputStream(png.size + metadata.size)
        val ihdr = parsed.firstOrNull() ?: return png
        out.write(signature)
        out.write(png, ihdr.start, ihdr.end - ihdr.start)
        out.write(metadata)
        parsed.drop(1).forEach { span ->
            if (!isOwnedMetadata(png, span)) out.write(png, span.start, span.end - span.start)
        }
        return out.toByteArray()
    }

    fun splice(png: ByteArray, length: Int, chunks: ByteArray, out: OutputStream) {
        if (length < ihdrEnd || length > png.size || !hasValidIhdr(png, length)) {
            out.write(png, 0, length.coerceIn(0, png.size))
            return
        }
        out.write(png, 0, ihdrEnd)
        out.write(chunks)
        out.write(png, ihdrEnd, length - ihdrEnd)
    }

    fun exif(takenAt: Long, description: String, location: GeoPoint?): ByteArray {
        val date = ExifText.dateTime(takenAt)
        val exifEntries = listOf(
            ascii(0x9003, date),
            ascii(0x9004, date),
            ascii(0x9291, ExifText.subSecond(takenAt))
        )
        val gps = location?.takeIf { it.valid }
        val gpsEntries = if (gps == null) emptyList() else listOf(
            Entry(0x0000, BYTE, 4, byteArrayOf(2, 3, 0, 0)),
            ascii(0x0001, if (gps.latitude < 0) "S" else "N"),
            Entry(0x0002, RATIONAL, 3, rationals(gps.latitude)),
            ascii(0x0003, if (gps.longitude < 0) "W" else "E"),
            Entry(0x0004, RATIONAL, 3, rationals(gps.longitude))
        )
        val ifd0Entries = mutableListOf(utf8(0x010E, description), ascii(0x0131, "Frame out"), long(0x8769, 0))
        if (gps != null) ifd0Entries += long(0x8825, 0)
        val ifd0Size = ifdSize(ifd0Entries.size, ifd0Entries)
        val exifOffset = 8 + ifd0Size
        val exifSize = ifdSize(exifEntries.size, exifEntries)
        val gpsOffset = exifOffset + exifSize
        ifd0Entries[2] = long(0x8769, exifOffset)
        if (gps != null) ifd0Entries[3] = long(0x8825, gpsOffset)
        val gpsSize = if (gps != null) ifdSize(gpsEntries.size, gpsEntries) else 0
        val buffer = ByteBuffer.allocate(gpsOffset + gpsSize).order(ByteOrder.BIG_ENDIAN)
        buffer.put('M'.code.toByte()).put('M'.code.toByte()).putShort(0x002A).putInt(8)
        writeIfd(buffer, 8, ifd0Entries)
        writeIfd(buffer, exifOffset, exifEntries)
        if (gps != null) writeIfd(buffer, gpsOffset, gpsEntries)
        return buffer.array()
    }

    private fun parseChunks(bytes: ByteArray): List<ChunkSpan>? {
        if (bytes.size < ihdrEnd || !signature.contentEquals(bytes.copyOfRange(0, signature.size))) return null
        val spans = ArrayList<ChunkSpan>()
        var pos = 8
        var sawIhdr = false
        var sawIend = false
        while (pos + 12 <= bytes.size) {
            val length = ByteBuffer.wrap(bytes, pos, 4).int
            if (length < 0) return null
            val dataStart = pos + 8
            val dataEnd = dataStart.toLong() + length
            val end = dataEnd + 4L
            if (dataEnd > bytes.size || end > bytes.size) return null
            val type = String(bytes, pos + 4, 4, Charsets.US_ASCII)
            if (!sawIhdr && type != "IHDR") return null
            if (type == "IHDR") {
                if (sawIhdr || length != 13) return null
                sawIhdr = true
            }
            if (!crcValid(bytes, pos + 4, length)) return null
            val span = ChunkSpan(type, pos, end.toInt(), dataStart, dataEnd.toInt())
            spans += span
            if (type == "IEND") {
                if (length != 0 || end.toInt() != bytes.size) return null
                sawIend = true
                break
            }
            pos = end.toInt()
        }
        return spans.takeIf { sawIhdr && sawIend }
    }

    private fun hasValidIhdr(bytes: ByteArray, length: Int): Boolean {
        if (length < ihdrEnd || bytes.size < ihdrEnd) return false
        if (!signature.contentEquals(bytes.copyOfRange(0, signature.size))) return false
        val ihdrLength = ByteBuffer.wrap(bytes, 8, 4).int
        if (ihdrLength != 13 || String(bytes, 12, 4, Charsets.US_ASCII) != "IHDR") return false
        return crcValid(bytes, 12, 13)
    }

    private fun crcValid(bytes: ByteArray, typeStart: Int, dataLength: Int): Boolean {
        val crcStart = typeStart + 4 + dataLength
        if (crcStart + 4 > bytes.size) return false
        val crc = CRC32().apply { update(bytes, typeStart, 4 + dataLength) }.value.toInt()
        return crc == ByteBuffer.wrap(bytes, crcStart, 4).int
    }

    private fun isOwnedMetadata(bytes: ByteArray, span: ChunkSpan): Boolean = when (span.type) {
        "eXIf" -> true
        "tEXt" -> keyword(bytes, span, 0) == "Software"
        "iTXt" -> keyword(bytes, span, 0) == "Description"
        else -> false
    }

    private fun keyword(bytes: ByteArray, span: ChunkSpan, zeroes: Int): String? {
        val max = span.dataEnd
        var end = span.dataStart
        while (end < max && bytes[end].toInt() != 0) end++
        if (end >= max || end - span.dataStart !in 1..79) return null
        if (zeroes > 0 && end + zeroes >= max) return null
        return String(bytes, span.dataStart, end - span.dataStart, Charsets.ISO_8859_1)
    }

    private fun ifdSize(count: Int, entries: List<Entry>): Int = 2 + 12 * count + 4 + entries.sumOf { extra(it) }
    private fun extra(entry: Entry): Int = if (entry.data.size > 4) entry.data.size + (entry.data.size and 1) else 0

    private fun writeIfd(buffer: ByteBuffer, offset: Int, entries: List<Entry>) {
        val sorted = entries.sortedBy { it.tag }
        buffer.position(offset)
        buffer.putShort(sorted.size.toShort())
        var dataOffset = offset + 2 + 12 * sorted.size + 4
        val tail = ArrayList<Pair<Int, ByteArray>>()
        sorted.forEach { entry ->
            buffer.putShort(entry.tag.toShort()).putShort(entry.type.toShort()).putInt(entry.count)
            if (entry.data.size <= 4) {
                buffer.put(entry.data)
                repeat(4 - entry.data.size) { buffer.put(0) }
            } else {
                buffer.putInt(dataOffset)
                tail += dataOffset to entry.data
                dataOffset += entry.data.size + (entry.data.size and 1)
            }
        }
        buffer.putInt(0)
        tail.forEach { (position, data) -> buffer.position(position); buffer.put(data) }
    }

    private fun ascii(tag: Int, text: String): Entry {
        val data = text.toByteArray(Charsets.US_ASCII) + 0
        return Entry(tag, ASCII, data.size, data)
    }

    private fun utf8(tag: Int, text: String): Entry {
        val data = text.toByteArray(Charsets.UTF_8) + 0
        return Entry(tag, ASCII, data.size, data)
    }

    private fun long(tag: Int, value: Int): Entry = Entry(tag, LONG, 1, ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array())

    private fun rationals(value: Double): ByteArray {
        val parts = GeoPoint.exifRationals(value)
        val buffer = ByteBuffer.allocate(24).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(parts[0].toInt()).putInt(1).putInt(parts[1].toInt()).putInt(1).putInt(parts[2].toInt()).putInt(10000)
        return buffer.array()
    }

    private fun international(keyword: String, text: String): ByteArray = ByteArrayOutputStream().apply {
        write(keyword.toByteArray(Charsets.ISO_8859_1)); write(0); write(0); write(0); write(0); write(0)
        write(text.toByteArray(Charsets.UTF_8))
    }.toByteArray()

    private fun chunk(type: String, data: ByteArray): ByteArray {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        val crc = CRC32().apply { update(typeBytes); update(data) }.value
        return ByteBuffer.allocate(12 + data.size).order(ByteOrder.BIG_ENDIAN).putInt(data.size).put(typeBytes).put(data).putInt(crc.toInt()).array()
    }
}
