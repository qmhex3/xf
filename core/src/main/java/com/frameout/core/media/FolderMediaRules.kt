package com.frameout.core.media

import com.frameout.codecpack.ContainerSniffer
import java.util.Locale

object FolderMediaRules {
    const val MIN_BYTES = 1024L

    private val sniffRequired = setOf("ts", "dat", "mod", "tod", "tp", "trp", "dv", "dif", "movie", "nut", "evo")
    private val mimes = mapOf(
        "mp4" to "video/mp4", "m4v" to "video/mp4", "mov" to "video/quicktime", "qt" to "video/quicktime",
        "3gp" to "video/3gpp", "3gpp" to "video/3gpp", "3g2" to "video/3gpp2", "mkv" to "video/x-matroska",
        "webm" to "video/webm", "avi" to "video/x-msvideo", "wmv" to "video/x-ms-wmv", "asf" to "video/x-ms-asf",
        "flv" to "video/x-flv", "ts" to "video/mp2t", "m2ts" to "video/mp2t", "mts" to "video/mp2t",
        "mpg" to "video/mpeg", "mpeg" to "video/mpeg", "ogv" to "video/ogg", "rm" to "application/vnd.rn-realmedia",
        "rmvb" to "application/vnd.rn-realmedia-vbr", "mxf" to "application/mxf", "gif" to "image/gif", "apng" to "image/apng"
    )

    fun kindOf(ext: String): MediaKind? = when {
        ext == "gif" -> MediaKind.Gif
        ext == "apng" -> MediaKind.Animated
        ext in ContainerSniffer.videoExtensions -> MediaKind.Video
        else -> null
    }

    fun needsSniff(ext: String): Boolean = ext in sniffRequired

    fun mimeOf(ext: String): String = mimes[ext] ?: "video/$ext"

    fun looksLikeVideo(head: ByteArray, length: Int): Boolean = ContainerSniffer.sniff(head, length)?.video == true

    fun skipDirectory(name: String, depth: Int): Boolean =
        name.startsWith(".") || name.equals("lost+found", true) || (depth == 1 && name.equals("Android", true))

    fun skipFile(name: String): Boolean = name.startsWith(".")

    fun bucketIdOf(parentPath: String): String = parentPath.lowercase(Locale.ROOT).hashCode().toString()
}
