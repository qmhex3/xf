package com.frameout.engine.decode

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureException
import com.frameout.engine.extract.isDiskFull

internal class SourceCopier(private val context: Context, private val cancelled: () -> Boolean) {
    var reuse = false
    private var cachedCopy: java.io.File? = null

    fun release(retain: Boolean) {
        if (!retain) runCatching { cachedCopy?.delete() }
        cachedCopy = null
    }

    fun copy(uri: Uri, descriptor: ParcelFileDescriptor): String? {
        val size = descriptor.statSize
        return try {
            val stable = if (reuse && size > 0L) java.io.File(context.cacheDir, "$COPY_PREFIX${uriKey(uri)}_$size$COPY_SUFFIX") else null
            sweepStaleCopies(stable)
            if (stable != null && stable.isFile && stable.length() == size) {
                stable.setLastModified(System.currentTimeMillis())
                cachedCopy = stable
                runCatching { descriptor.close() }
                return stable.absolutePath
            }
            ensureSpace(size)
            val target = stable ?: java.io.File.createTempFile(COPY_PREFIX, COPY_SUFFIX, context.cacheDir)
            val part = if (stable != null) java.io.File(stable.path + PART_SUFFIX) else target
            try {
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                    part.outputStream().use { output -> copyCancellable(input, output) }
                }
                if (part !== target) {
                    target.delete()
                    if (!part.renameTo(target)) throw java.io.IOException("Could not finish source copy")
                }
            } catch (error: Throwable) {
                part.delete()
                throw error
            }
            cachedCopy = target
            target.absolutePath
        } catch (error: Throwable) {
            runCatching { descriptor.close() }
            if (error is java.util.concurrent.CancellationException || error.isDiskFull()) throw error
            throw MediaFailureException(MediaFailure.IoError, "Could not copy the source to cache: ${error.javaClass.simpleName}: ${error.message}", error)
        }
    }

    private fun uriKey(uri: Uri): String = java.security.MessageDigest.getInstance("SHA-1").digest(uri.toString().toByteArray()).take(10).joinToString("") { "%02x".format(it) }
    private fun copyCancellable(input: java.io.InputStream, output: java.io.OutputStream) {
        val buffer = ByteArray(COPY_BUFFER)
        while (true) {
            if (cancelled()) throw java.util.concurrent.CancellationException("Source copy cancelled")
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
        }
    }

    private fun ensureSpace(size: Long) {
        if (size <= 0L) return
        val free = android.os.StatFs(context.cacheDir.path).availableBytes
        if (free < size + SPACE_MARGIN) throw java.io.IOException("No space left on device for the source copy")
    }

    private fun sweepStaleCopies(keep: java.io.File?) {
        val now = System.currentTimeMillis()
        val files = context.cacheDir.listFiles { file -> file.name.startsWith(COPY_PREFIX) } ?: return
        for (file in files) {
            if (file == cachedCopy || file == keep || (keep != null && file.path == keep.path + PART_SUFFIX)) continue
            val limit = if (file.name.endsWith(PART_SUFFIX)) PART_STALE_MS else COPY_STALE_MS
            if (now - file.lastModified() > limit) file.delete()
        }
    }

    private companion object {
        const val COPY_PREFIX = "ffmpeg_src_"
        const val COPY_SUFFIX = ".bin"
        const val PART_SUFFIX = ".part"
        const val COPY_BUFFER = 256 * 1024
        const val SPACE_MARGIN = 64L * 1024L * 1024L
        const val COPY_STALE_MS = 6L * 60L * 60L * 1000L
        const val PART_STALE_MS = 10L * 60L * 1000L
    }
}
