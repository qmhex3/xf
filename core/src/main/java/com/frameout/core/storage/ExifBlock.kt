package com.frameout.core.storage

import android.graphics.Bitmap
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ExifBlock {
    private val HEADER = byteArrayOf('E'.code.toByte(), 'x'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), 0, 0)

    fun build(stamp: ExifStamp): ByteArray? = runCatching {
        val file = File.createTempFile("fx_exif", ".jpg")
        try {
            val seed = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            try {
                FileOutputStream(file).use { check(seed.compress(Bitmap.CompressFormat.JPEG, 50, it)) }
            } finally {
                seed.recycle()
            }
            applyExifTags(ExifInterface(file.absolutePath), stamp)
            extract(file.readBytes())
        } finally {
            file.delete()
        }
    }.getOrNull()

    fun extract(jpeg: ByteArray): ByteArray? {
        if (jpeg.size < 4 || jpeg[0] != MARKER_PREFIX || jpeg[1] != SOI) return null
        var pos = 2
        while (pos < jpeg.size) {
            while (pos < jpeg.size && jpeg[pos] == MARKER_PREFIX) pos++
            if (pos >= jpeg.size) return null
            val marker = jpeg[pos].toInt() and 0xFF
            pos++
            if (marker == EOI) return null
            if (marker == START_OF_SCAN) return null
            if (isStandalone(marker)) continue
            if (pos + 2 > jpeg.size) return null
            val length = ((jpeg[pos].toInt() and 0xFF) shl 8) or (jpeg[pos + 1].toInt() and 0xFF)
            if (length < 2) return null
            val start = pos + 2
            val end = start + length - 2
            if (end > jpeg.size) return null
            if (marker == APP1 && hasHeader(jpeg, start, end)) return jpeg.copyOfRange(start, end)
            pos = end
        }
        return null
    }

    private fun hasHeader(data: ByteArray, start: Int, end: Int): Boolean =
        end - start >= HEADER.size && HEADER.indices.all { data[start + it] == HEADER[it] }

    private fun isStandalone(marker: Int): Boolean = marker in 0xD0..0xD7 || marker == 0x01

    private const val MARKER_PREFIX: Byte = 0xFF.toByte()
    private const val SOI: Byte = 0xD8.toByte()
    private const val EOI = 0xD9
    private const val APP1 = 0xE1
    private const val START_OF_SCAN = 0xDA
}
