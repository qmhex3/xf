package com.frameout.engine.decode

import android.os.ParcelFileDescriptor
import android.system.Os

class PfdRangeReader(private val descriptor: ParcelFileDescriptor) : ApngRangeReader {
    override fun read(offset: Long, length: Int): ByteArray {
        val buffer = ByteArray(length)
        var done = 0
        while (done < length) {
            val n = Os.pread(descriptor.fileDescriptor, buffer, done, length - done, offset + done)
            if (n <= 0) error("Unexpected APNG EOF")
            done += n
        }
        return buffer
    }

    fun close() {
        runCatching { descriptor.close() }
    }
}
