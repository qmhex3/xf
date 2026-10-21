package com.frameout.core.util

import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.RandomAccessFile

interface DiagSink {
    fun write(batch: List<String>)
    fun tail(maxBytes: Int): String
}

class DiagFileSink(private val file: File, private val maxBytes: Long = 512_000L) : DiagSink {
    override fun write(batch: List<String>) {
        if (batch.isEmpty()) return
        try {
            file.parentFile?.mkdirs()
            FileWriter(file, true).use { writer -> batch.forEach { writer.append(it).append('\n') } }
            if (file.length() > maxBytes) rotate()
        } catch (_: IOException) {
        }
    }

    override fun tail(maxBytes: Int): String {
        if (!file.exists()) return ""
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val start = (raf.length() - maxBytes).coerceAtLeast(0L)
                val bytes = ByteArray((raf.length() - start).toInt())
                raf.seek(start)
                raf.readFully(bytes)
                val text = String(bytes, Charsets.UTF_8)
                if (start > 0L) text.substringAfter('\n', "") else text
            }
        } catch (_: IOException) {
            ""
        }
    }

    private fun rotate() {
        val backup = File(file.parentFile, file.nameWithoutExtension + ".prev.log")
        backup.delete()
        file.renameTo(backup)
    }
}
