package com.frameout.engine.decode

import android.content.Context
import android.net.Uri
import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import androidx.annotation.RequiresApi
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

@RequiresApi(26)
internal class StreamingProxySource(
    private val context: Context,
    private val uri: Uri,
    private val totalSize: Long
) : ProxyFileDescriptorCallback() {
    private val lock = Any()
    private var input: InputStream? = null
    private var inputPos = 0L
    private val scratch = ByteArray(256 * 1024)
    private val blocks = object : LinkedHashMap<Long, ByteArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ByteArray>): Boolean = size > MAX_BLOCKS
    }

    override fun onGetSize(): Long = totalSize

    override fun onRead(offset: Long, size: Int, data: ByteArray): Int = synchronized(lock) {
        try {
            read(offset, size, data)
        } catch (error: IOException) {
            throw ErrnoException("read", OsConstants.EIO)
        }
    }

    override fun onRelease() {
        synchronized(lock) {
            runCatching { input?.close() }
            input = null
            blocks.clear()
        }
    }

    private fun read(offset: Long, size: Int, data: ByteArray): Int {
        if (offset < 0L || offset >= totalSize) return 0
        val want = minOf(size.toLong(), totalSize - offset).toInt()
        var copied = 0
        var pos = offset
        while (copied < want) {
            val index = pos / BLOCK
            val block = block(index)
            val inBlock = (pos - index * BLOCK).toInt()
            if (inBlock >= block.size) break
            val n = minOf(block.size - inBlock, want - copied)
            System.arraycopy(block, inBlock, data, copied, n)
            copied += n
            pos += n
        }
        return copied
    }

    private fun block(index: Long): ByteArray {
        blocks[index]?.let { return it }
        val start = index * BLOCK
        positionAt(start)
        val len = minOf(BLOCK.toLong(), totalSize - start).toInt()
        val buffer = ByteArray(len)
        val stream = input ?: throw IOException("no stream")
        var got = 0
        while (got < len) {
            val n = stream.read(buffer, got, len - got)
            if (n < 0) break
            got += n
        }
        inputPos = start + got
        val result = if (got == len) buffer else buffer.copyOf(got)
        blocks[index] = result
        return result
    }

    private fun positionAt(target: Long) {
        if (input != null && inputPos <= target) {
            discard(target - inputPos)
            return
        }
        runCatching { input?.close() }
        input = context.contentResolver.openInputStream(uri) ?: throw IOException("no stream")
        inputPos = 0L
        discard(target)
    }

    private fun discard(count: Long) {
        val stream = input ?: throw IOException("no stream")
        var left = count
        while (left > 0L) {
            val skipped = stream.skip(left)
            if (skipped > 0L) {
                left -= skipped
                continue
            }
            val n = stream.read(scratch, 0, minOf(left, scratch.size.toLong()).toInt())
            if (n < 0) throw EOFException()
            left -= n
        }
        inputPos += count
    }

    private companion object {
        const val BLOCK = 4 * 1024 * 1024
        const val MAX_BLOCKS = 24
    }
}
