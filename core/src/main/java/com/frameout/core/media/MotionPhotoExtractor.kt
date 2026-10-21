package com.frameout.core.media

import android.content.Context
import android.net.Uri
import com.frameout.core.util.logged
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

object MotionPhotoExtractor {
    private const val MAX_VERDICTS = 4096

    private val motionVerdicts = object : LinkedHashMap<String, Boolean>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean = size > MAX_VERDICTS
    }

    private val failedExtractions = object : LinkedHashMap<String, Boolean>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean = size > MAX_VERDICTS
    }

    private const val VERDICT_SPACE = "motion"

    private fun verdict(context: Context, key: String): Boolean? =
        synchronized(motionVerdicts) { motionVerdicts[key] } ?: ProbeStore.get(context, VERDICT_SPACE, key)?.also { synchronized(motionVerdicts) { motionVerdicts[key] = it } }

    private fun remember(context: Context, key: String, value: Boolean) {
        synchronized(motionVerdicts) { motionVerdicts[key] = value }
        ProbeStore.put(context, VERDICT_SPACE, key, value)
    }

    private fun hasFailed(key: String): Boolean = synchronized(failedExtractions) { failedExtractions[key] == true }

    private fun markFailed(key: String) { synchronized(failedExtractions) { failedExtractions[key] = true } }

    fun isMotionCandidate(mime: String, name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return mime.equals("image/jpeg", true) || mime.equals("image/heic", true) || mime.equals("image/heif", true) ||
            ext == "jpg" || ext == "jpeg" || ext == "heic" || ext == "heif"
    }

    fun isMotionPhotoQuick(context: Context, uri: Uri): Boolean {
        val key = "q" + cacheKey(context, uri)
        verdict(context, key)?.let { return it }
        var conclusive = false
        val result = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = ByteArray(128 * 1024)
                var used = 0
                while (used < bytes.size) {
                    val read = stream.read(bytes, used, bytes.size - used)
                    if (read <= 0) break
                    used += read
                }
                conclusive = true
                MotionPhotoXmp.hasMotionMarkers(MotionPhotoXmp.extractXmp(bytes.copyOf(used)))
            } ?: false
        }.getOrDefault(false)
        if (conclusive) remember(context, key, result)
        return result
    }

    fun isMotionPhoto(context: Context, uri: Uri): Boolean {
        val key = cacheKey(context, uri)
        verdict(context, key)?.let { return it }
        var conclusive = false
        val result = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = ByteArray(512 * 1024)
                var used = 0
                while (used < bytes.size) {
                    val read = stream.read(bytes, used, bytes.size - used)
                    if (read <= 0) break
                    used += read
                }
                conclusive = true
                MotionPhotoXmp.hasMotionMarkers(MotionPhotoXmp.extractXmp(bytes.copyOf(used)))
            } ?: false
        }.getOrDefault(false)
        if (conclusive) remember(context, key, result)
        return result
    }

    fun isLikelyMotionName(name: String): Boolean {
        val n = name.lowercase()
        return n.startsWith("mvimg") || n.contains(".mp.") || n.endsWith(".mp.jpg") || n.endsWith("_mp.jpg")
    }

    fun extractToCache(context: Context, uri: Uri): File? {
        val key = runCatching { cacheKey(context, uri) }.getOrNull() ?: return null
        val out = File(context.cacheDir, "motion/$key.mp4")
        if (out.exists() && out.length() > 1000 && validMp4(out)) {
            out.setLastModified(System.currentTimeMillis())
            return out
        }
        if (hasFailed(key)) return null
        var definitive = false
        val result = runCatching {
            val temp = File(out.parentFile, "${out.name}.${System.nanoTime()}.tmp")
            try {
                when (copyEmbedded(context, uri, temp)) {
                    Probe.Unavailable -> return@runCatching null
                    Probe.NoVideo -> {
                        definitive = true
                        return@runCatching null
                    }
                    Probe.Copied -> Unit
                }
                if (temp.length() < 32 || !validMp4(temp)) {
                    definitive = true
                    return@runCatching null
                }
                if (out.exists()) out.delete()
                check(temp.renameTo(out))
                out
            } finally {
                temp.delete()
            }
        }.logged("MotionPhotoExtractor").getOrNull()
        if (result == null && definitive) markFailed(key)
        return result
    }

    private enum class Probe { Copied, NoVideo, Unavailable }

    private fun copyEmbedded(context: Context, uri: Uri, temp: File): Probe {
        val probe = context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).channel.use { source ->
                val size = source.size()
                val start = if (size < 64) null else findEmbeddedVideoStart(source, size)
                if (start == null) {
                    Probe.NoVideo
                } else {
                    copyRange(source, size, start, temp)
                    Probe.Copied
                }
            }
        }
        return probe ?: Probe.Unavailable
    }

    private fun findEmbeddedVideoStart(source: java.nio.channels.FileChannel, size: Long): Long? {
        val headSize = minOf(size, 512L * 1024L).toInt()
        val head = ByteBuffer.allocate(headSize)
        source.position(0)
        while (head.hasRemaining() && source.read(head) > 0) Unit
        val xmp = MotionPhotoXmp.extractXmp(head.array())
        val declared = xmp?.let { x ->
            val video = MotionPhotoXmp.parseItems(x).lastOrNull { it.isVideo && it.length > 0 }
            video?.length ?: MotionPhotoXmp.parseMicroOffset(x)
        }
        if (declared != null && declared in 9 until size) {
            val base = size - declared
            for (candidate in longArrayOf(base, base + 8)) {
                if (candidate in 1 until size - 8 && ftypAt(source, candidate)) return candidate
            }
        }
        return scanFtypInChannel(source, size)
    }

    private fun ftypAt(source: java.nio.channels.FileChannel, start: Long): Boolean {
        val buffer = ByteBuffer.allocate(8)
        var position = start
        while (buffer.hasRemaining()) {
            val read = source.read(buffer, position)
            if (read <= 0) return false
            position += read
        }
        val raw = buffer.array()
        return raw[4] == 0x66.toByte() && raw[5] == 0x74.toByte() && raw[6] == 0x79.toByte() && raw[7] == 0x70.toByte()
    }

    private fun scanFtypInChannel(source: java.nio.channels.FileChannel, size: Long): Long? {
        val chunk = 4 * 1024 * 1024
        val overlap = 16
        var end = size
        while (end > 32) {
            val begin = maxOf(0L, end - chunk)
            val length = (end - begin).toInt()
            val buffer = ByteBuffer.allocate(length)
            var position = begin
            while (buffer.hasRemaining()) {
                val read = source.read(buffer, position)
                if (read <= 0) break
                position += read
            }
            val bytes = buffer.array()
            for (i in length - 4 downTo 4) {
                if (bytes[i] == 0x66.toByte() && bytes[i + 1] == 0x74.toByte() && bytes[i + 2] == 0x79.toByte() && bytes[i + 3] == 0x70.toByte()) {
                    val start = begin + i - 4
                    if (start > 16 && plausibleFtypSize(bytes, i - 4)) return start
                }
            }
            if (begin == 0L) break
            end = begin + overlap
        }
        return null
    }

    private fun plausibleFtypSize(bytes: ByteArray, at: Int): Boolean {
        val boxSize = ((bytes[at].toInt() and 0xff) shl 24) or ((bytes[at + 1].toInt() and 0xff) shl 16) or ((bytes[at + 2].toInt() and 0xff) shl 8) or (bytes[at + 3].toInt() and 0xff)
        return boxSize in 12..512
    }

    private fun copyRange(source: java.nio.channels.FileChannel, size: Long, start: Long, out: File) {
        if (start <= 0 || start >= size - 8) return
        out.parentFile?.mkdirs()
        source.position(start)
        FileOutputStream(out).channel.use { target ->
            var position = start
            while (position < size) {
                val moved = source.transferTo(position, minOf(8L * 1024L * 1024L, size - position), target)
                if (moved <= 0) error("Motion photo copy incomplete")
                position += moved
            }
        }
    }

    private fun cacheKey(context: Context, uri: Uri): String {
        val meta = runCatching { context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.SIZE, android.provider.MediaStore.MediaColumns.DATE_MODIFIED), null, null, null)?.use { if (it.moveToFirst()) "${it.getLong(0)}:${it.getLong(1)}" else "" } }.getOrDefault("")
        return MessageDigest.getInstance("SHA-256").digest("$uri|$meta".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun validMp4(file: File): Boolean = runCatching {
        java.io.RandomAccessFile(file, "r").use { raf ->
            if (raf.length() < 12) return false
            raf.seek(4)
            ByteArray(4).also(raf::readFully).contentEquals(byteArrayOf(0x66, 0x74, 0x79, 0x70))
        }
    }.getOrDefault(false)

    fun findVideoStart(bytes: ByteArray): Int? {
        val xmp = MotionPhotoXmp.extractXmp(bytes)
        xmp?.let { x -> MotionPhotoXmp.parseItems(x).lastOrNull { it.isVideo && it.length > 0 }?.length?.let { len -> videoStart(bytes.size.toLong() - len, bytes) } }?.let { return it }
        xmp?.let { MotionPhotoXmp.parseMicroOffset(it)?.let { off -> videoStart(bytes.size.toLong() - off, bytes) } }?.let { return it }
        return scanFtyp(bytes)
    }

    private fun videoStart(start: Long, bytes: ByteArray): Int? = start.toInt().takeIf { start > 0 && start <= Int.MAX_VALUE && looksLikeMp4(bytes, it) }

    private fun scanFtyp(bytes: ByteArray): Int? {
        val sig = byteArrayOf(0x66, 0x74, 0x79, 0x70)
        for (i in bytes.size - 32 downTo 32) if (bytes[i] == sig[0] && bytes[i + 1] == sig[1] && bytes[i + 2] == sig[2] && bytes[i + 3] == sig[3]) {
            val start = i - 4
            if (start >= 0 && looksLikeMp4(bytes, start)) return start
        }
        return null
    }

    private fun looksLikeMp4(bytes: ByteArray, start: Int): Boolean = start + 8 <= bytes.size && bytes.copyOfRange(start + 4, start + 8).contentEquals(byteArrayOf('f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte()))

    fun resolveVideoSource(context: Context, uri: Uri): Uri {
        val name = displayNameOf(context, uri) ?: uri.lastPathSegment.orEmpty()
        val extension = name.substringAfterLast('.', "").lowercase()
        val mime = context.contentResolver.getType(uri).orEmpty()
        val imageLike = mime.startsWith("image/") || extension in setOf("jpg", "jpeg", "heic", "heif")
        if (!imageLike && !isLikelyMotionName(name)) return uri
        if (isLikelyMotionName(name) || isMotionPhoto(context, uri)) {
            extractToCache(context, uri)?.let { return Uri.fromFile(it) }
        }
        return findLivePair(context, uri, name, mime) ?: uri
    }

    private fun displayNameOf(context: Context, uri: Uri): String? = runCatching {
        if (uri.scheme != "content") return@runCatching null
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()?.ifBlank { null }

    fun findLivePair(context: Context, imageUri: Uri, displayName: String, mime: String = ""): Uri? = LivePhotoMatcher.findPair(context, imageUri, displayName, mime)
}
