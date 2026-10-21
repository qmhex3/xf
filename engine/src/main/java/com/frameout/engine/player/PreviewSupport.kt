package com.frameout.engine.player

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.frameout.codecpack.ContainerSniffer

object PreviewSupport {
    private val containers = setOf("realmedia", "mxf")
    private val extensions = setOf("rm", "rmvb", "mxf")

    internal fun exoCannotPlay(container: String?, extension: String): Boolean =
        container in containers || (container == null && extension.lowercase() in extensions)

    fun exoCannotPlay(context: Context, uri: Uri): Boolean = runCatching {
        exoCannotPlay(sniffContainer(context, uri), extensionOf(context, uri))
    }.getOrDefault(false)

    private fun sniffContainer(context: Context, uri: Uri): String? = context.contentResolver.openInputStream(uri)?.use { input ->
        val head = ByteArray(ContainerSniffer.HEAD_BYTES)
        var read = 0
        while (read < head.size) {
            val n = input.read(head, read, head.size - read)
            if (n < 0) break
            read += n
        }
        ContainerSniffer.sniff(head, read)?.container
    }

    private fun extensionOf(context: Context, uri: Uri): String {
        val name = if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
            }.getOrNull()
        } else null
        return ContainerSniffer.extensionOf(name ?: uri.lastPathSegment.orEmpty())
    }
}
