package com.frameout.codecpack

data class Sniffed(val container: String, val video: Boolean)

object ContainerSniffer {
    const val HEAD_BYTES = 400

    val videoExtensions: Set<String> = setOf(
        "mp4", "m4v", "mov", "qt", "3gp", "3g2", "3gpp", "3gpp2", "mkv", "mk3d", "webm", "avi", "wmv", "asf",
        "flv", "f4v", "ts", "m2ts", "mts", "m2t", "tp", "trp", "mpg", "mpeg", "mpe", "m1v", "m2v", "vob", "evo",
        "dat", "mod", "tod", "ogv", "ogm", "ogx", "rm", "rmvb", "mxf", "divx", "xvid", "dv", "dif", "amv", "nut",
        "ivf", "y4m", "mj2", "mjp2", "dnxhd", "dnxhr", "wtv", "dvr-ms", "gxf", "lxf", "bik", "smk", "mjpeg", "mjpg", "hevc", "h265", "h264", "264",
        "265", "avc", "vc1", "movie", "lrv", "insv", "mp4v"
    )

    val systemExtensions: Set<String> = setOf("mp4", "m4v", "mov", "qt", "3gp", "3g2", "3gpp", "3gpp2", "mkv", "webm", "ts", "m2ts", "mts", "lrv", "insv")

    fun isVideoName(name: String): Boolean = extensionOf(name) in videoExtensions

    fun extensionOf(name: String): String = name.substringAfterLast('.', "").lowercase()

    fun sniff(head: ByteArray, length: Int = head.size): Sniffed? {
        val n = minOf(length, head.size)
        if (n < 4) return null
        if (startsWith(head, n, 0, 0x1A, 0x45, 0xDF, 0xA3)) return Sniffed("matroska", true)
        if (ascii(head, n, 0, "RIFF") && n >= 12) {
            return when {
                ascii(head, n, 8, "AVI ") -> Sniffed("avi", true)
                ascii(head, n, 8, "WEBP") -> Sniffed("webp", false)
                else -> null
            }
        }
        if (ascii(head, n, 0, "FLV") && n > 3 && head[3].toInt() == 1) return Sniffed("flv", true)
        if (startsWith(head, n, 0, 0x30, 0x26, 0xB2, 0x75, 0x8E, 0x66, 0xCF, 0x11)) return Sniffed("asf", true)
        if (startsWith(head, n, 0, 0x00, 0x00, 0x01, 0xBA)) return Sniffed("mpeg-ps", true)
        if (startsWith(head, n, 0, 0x00, 0x00, 0x01, 0xB3)) return Sniffed("mpeg-video", true)
        if (ascii(head, n, 0, "OggS")) return Sniffed("ogg", true)
        if (ascii(head, n, 0, ".RMF")) return Sniffed("realmedia", true)
        if (startsWith(head, n, 0, 0x06, 0x0E, 0x2B, 0x34, 0x02, 0x05, 0x01, 0x01, 0x0D, 0x01, 0x02, 0x01)) return Sniffed("mxf", true)
        if (isDv(head, n)) return Sniffed("dv", true)
        if (ascii(head, n, 0, "nut/multimedia container")) return Sniffed("nut", true)
        if (ascii(head, n, 0, "DKIF")) return Sniffed("ivf", true)
        if (ascii(head, n, 0, "YUV4MPEG2")) return Sniffed("y4m", true)
        if (ascii(head, n, 0, "GIF8")) return Sniffed("gif", false)
        if (startsWith(head, n, 0, 0x89, 0x50, 0x4E, 0x47)) return Sniffed("png", false)
        if (ascii(head, n, 4, "ftyp")) return isoBrand(head, n)
        if (isTransportStream(head, n)) return Sniffed("mpeg-ts", true)
        return null
    }

    private fun isDv(head: ByteArray, n: Int): Boolean {
        if (n < 84 || !startsWith(head, n, 0, 0x1F, 0x07, 0x00)) return false
        return (head[3].toInt() and 0x7F) == 0x3F && (head[80].toInt() and 0xFF) == 0x3F
    }

    private fun isoBrand(head: ByteArray, n: Int): Sniffed {
        if (n < 12) return Sniffed("mp4", true)
        val brand = String(head, 8, 4, Charsets.ISO_8859_1)
        return when {
            brand == "qt  " -> Sniffed("mov", true)
            brand == "mjp2" || brand == "mj2s" -> Sniffed("mj2", true)
            brand == "avif" || brand == "avis" -> Sniffed("avif", false)
            brand in setOf("heic", "heix", "hevc", "hevx", "mif1", "msf1", "heim", "heis") -> Sniffed("heif", false)
            else -> Sniffed("mp4", true)
        }
    }

    private fun isTransportStream(head: ByteArray, n: Int): Boolean {
        fun sync(offset: Int) = offset < n && head[offset].toInt() == 0x47
        return (sync(0) && sync(188) && (n <= 376 || sync(376))) || (sync(4) && sync(196) && (n <= 388 || sync(388)))
    }

    private fun ascii(head: ByteArray, n: Int, offset: Int, text: String): Boolean {
        if (offset + text.length > n) return false
        for (i in text.indices) if (head[offset + i].toInt() != text[i].code) return false
        return true
    }

    private fun startsWith(head: ByteArray, n: Int, offset: Int, vararg bytes: Int): Boolean {
        if (offset + bytes.size > n) return false
        for (i in bytes.indices) if ((head[offset + i].toInt() and 0xFF) != bytes[i]) return false
        return true
    }
}
