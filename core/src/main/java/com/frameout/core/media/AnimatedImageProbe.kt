package com.frameout.core.media

import android.content.Context
import android.net.Uri
import com.frameout.codecpack.ApngParser

object AnimatedImageProbe {
    private const val SPACE = "animated"

    fun isCandidate(mime: String, name: String): Boolean =
        isWebp(mime, name) || mime.equals("image/apng", true) || name.endsWith(".apng", true)

    fun isPngCandidate(mime: String, name: String): Boolean =
        mime.equals("image/png", true) || name.endsWith(".png", true)

    fun isAnimated(context: Context, uri: Uri, mime: String, name: String, size: Long, stamp: Long): Boolean {
        val key = "$uri|$size|$stamp"
        ProbeStore.get(context, SPACE, key)?.let { return it }
        var conclusive = false
        val result = probe(context, uri, mime, name) { conclusive = true }
        if (conclusive) ProbeStore.put(context, SPACE, key, result)
        return result
    }

    internal fun webpHeaderAnimated(head: ByteArray, length: Int): Boolean {
        if (length < 21) return false
        fun tag(offset: Int, text: String) = text.indices.all { head[offset + it] == text[it].code.toByte() }
        return tag(0, "RIFF") && tag(8, "WEBP") && tag(12, "VP8X") && (head[20].toInt() and 0x02) != 0
    }

    private fun isWebp(mime: String, name: String) = mime.equals("image/webp", true) || name.endsWith(".webp", true)

    private fun probe(context: Context, uri: Uri, mime: String, name: String, onConclusive: () -> Unit): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.buffered()?.use { input ->
            onConclusive()
            if (isWebp(mime, name)) {
                val head = ByteArray(32)
                var read = 0
                while (read < head.size) {
                    val n = input.read(head, read, head.size - read)
                    if (n < 0) break
                    read += n
                }
                webpHeaderAnimated(head, read)
            } else {
                ApngParser.isAnimated(input)
            }
        } ?: false
    }.getOrDefault(false)
}
