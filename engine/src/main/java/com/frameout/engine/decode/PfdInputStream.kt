package com.frameout.engine.decode

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import java.io.IOException
import java.io.InputStream

internal class PfdInputStream(private val descriptor: ParcelFileDescriptor) : InputStream() {
    private var position = 0L

    override fun read(): Int {
        val one = ByteArray(1)
        return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xFF
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val n = try {
            Os.pread(descriptor.fileDescriptor, buffer, offset, length, position)
        } catch (e: ErrnoException) {
            throw IOException(e)
        }
        if (n <= 0) return -1
        position += n
        return n
    }

    override fun skip(count: Long): Long {
        if (count <= 0L) return 0L
        position += count
        return count
    }

    override fun close() = Unit
}
