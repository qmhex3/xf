package com.frameout.core.media

import java.nio.charset.Charset

internal object MotionPhotoXmp {
    internal fun hasMotionMarkers(xmp: String?): Boolean {
        if (xmp == null) return false
        if (Regex("""MotionPhoto[=:>\s]*"?\s*([01])""", RegexOption.IGNORE_CASE).find(xmp)?.groupValues?.get(1) == "0") return false
        if (Regex("""(?<![A-Za-z])MotionPhoto[=:>\s]*"?\s*1""", RegexOption.IGNORE_CASE).containsMatchIn(xmp)) return true
        if (Regex("""MicroVideo[=:>\s]*"?\s*1""", RegexOption.IGNORE_CASE).containsMatchIn(xmp)) return true
        return parseItems(xmp).any { it.isVideo }
    }

    internal data class ContainerItem(val mime: String, val semantic: String, val length: Long, val padding: Long) {
        val isVideo: Boolean get() = semantic.equals("MotionPhoto", true) || mime.startsWith("video/", true)
    }

    private val itemRegex = Regex("""<Container:Item\b(.*?)(?:/>|</Container:Item>)""", setOf(RegexOption.DOT_MATCHES_ALL))

    private fun itemField(block: String, name: String): String? =
        Regex("""Item:$name\s*=\s*"([^"]*)"""").find(block)?.groupValues?.get(1)
            ?: Regex("""<Item:$name>\s*([^<]*?)\s*</Item:$name>""").find(block)?.groupValues?.get(1)

    internal fun parseItems(xmp: String): List<ContainerItem> = itemRegex.findAll(xmp).map { m ->
        val block = m.groupValues[1]
        ContainerItem(
            mime = itemField(block, "Mime").orEmpty(),
            semantic = itemField(block, "Semantic").orEmpty(),
            length = itemField(block, "Length")?.trim()?.toLongOrNull() ?: 0L,
            padding = itemField(block, "Padding")?.trim()?.toLongOrNull() ?: 0L
        )
    }.toList()

    internal fun extractXmp(bytes: ByteArray): String? {
        val marker = "http://ns.google.com/photos/1.0/"
        val text = String(bytes, 0, minOf(bytes.size, 512 * 1024), Charset.forName("ISO-8859-1"))
        val i = text.indexOf(marker)
        if (i < 0) return null
        val end = text.indexOf("</x:xmpmeta>", i).let { if (it > 0) it + 12 else minOf(i + 8000, text.length) }
        return text.substring(i, end)
    }

    internal fun parseMicroOffset(xmp: String): Long? = Regex("""MicroVideoOffset[=:]\"?(\d+)""").find(xmp)?.groupValues?.getOrNull(1)?.toLongOrNull()?.takeIf { it > 1000 }
}
