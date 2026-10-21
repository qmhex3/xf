package com.frameout.engine.decode

import android.content.Context
import android.net.Uri
import com.frameout.codecpack.ContainerSniffer
import com.frameout.codecpack.WebpAnimParser

object ImageHead {
    enum class Kind { AnimatedWebp, Avif, Other }

    private const val HEAD_BYTES = 32

    fun kindOf(bytes: ByteArray, length: Int): Kind = when {
        WebpAnimParser.isAnimatedHeader(bytes, length) -> Kind.AnimatedWebp
        ContainerSniffer.sniff(bytes, length)?.container == "avif" -> Kind.Avif
        else -> Kind.Other
    }

    fun read(context: Context, uri: Uri): Kind? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = ByteArray(HEAD_BYTES)
            var read = 0
            while (read < bytes.size) {
                val n = input.read(bytes, read, bytes.size - read)
                if (n < 0) break
                read += n
            }
            kindOf(bytes, read)
        }
    }.getOrNull()

    fun isAnimatedWebp(context: Context, uri: Uri): Boolean = read(context, uri) == Kind.AnimatedWebp
}
