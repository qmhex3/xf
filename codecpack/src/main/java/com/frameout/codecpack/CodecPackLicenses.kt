package com.frameout.codecpack

import android.content.Context
import java.io.File

data class CodecPackLicense(val name: String, val spdx: String, val version: String?, val text: String?)

data class CodecPackBuildInfo(val installed: Boolean, val serial: Long?, val licenses: List<CodecPackLicense>)

object CodecPackLicenses {
    internal const val FFMPEG_TEXT = "COPYING.LGPLv2.1"
    internal const val DAV1D_TEXT = "COPYING.dav1d"
    internal val TEXT_FILES = arrayOf(FFMPEG_TEXT, DAV1D_TEXT)
    private const val MAX_TEXT_BYTES = 256L * 1024L
    private val FFMPEG_LINE = Regex("(?m)^FFmpeg (\\d[\\w.+-]*)\\s*$")
    private val DAV1D_LINE = Regex("(?m)^libdav1d (\\d[\\w.+-]*)(?:\\s.*)?$")

    internal const val BUNDLED_VERSIONS = "BUNDLED_VERSIONS.txt"

    fun read(context: Context): CodecPackBuildInfo {
        if (CodecPack.isBundled(context)) return fromAssets(context)
        val dir = runCatching { CodecPack.abiDir(context) }.getOrNull()
        if (dir == null || !CodecPack.isInstalled(context)) return parse(null, emptyMap(), false)
        return fromDirectory(dir)
    }

    private fun fromAssets(context: Context): CodecPackBuildInfo {
        val texts = TEXT_FILES.mapNotNull { name -> readAsset(context, name)?.let { name to it } }.toMap()
        return parse(readAsset(context, BUNDLED_VERSIONS), texts, true)
    }

    private fun readAsset(context: Context, name: String): String? =
        runCatching { context.assets.open(name).use { it.readBytes() } }.getOrNull()
            ?.takeIf { it.size in 1..MAX_TEXT_BYTES.toInt() }
            ?.toString(Charsets.UTF_8)

    internal fun fromDirectory(dir: File): CodecPackBuildInfo {
        val payload = readSmall(File(dir, CodecPack.PAYLOAD))
        val texts = TEXT_FILES.mapNotNull { name -> readSmall(File(dir, name))?.let { name to it } }.toMap()
        return parse(payload, texts, true)
    }

    internal fun parse(payload: String?, texts: Map<String, String>, installed: Boolean): CodecPackBuildInfo {
        val source = payload.orEmpty()
        val ffmpeg = FFMPEG_LINE.find(source)?.groupValues?.get(1)
        val dav1d = DAV1D_LINE.find(source)?.groupValues?.get(1)
        val serial = if (payload == null) null else CodecPackSerial.parse(source)
        return CodecPackBuildInfo(
            installed,
            serial,
            listOf(
                CodecPackLicense("FFmpeg", "LGPL-2.1-or-later", ffmpeg, texts[FFMPEG_TEXT]),
                CodecPackLicense("dav1d", "BSD-2-Clause", dav1d, texts[DAV1D_TEXT])
            )
        )
    }

    fun paragraphs(text: String): List<String> =
        text.replace("\r\n", "\n").split(Regex("\n\\s*\n"))
            .map { block -> block.lines().joinToString(" ") { it.trim() }.trim() }
            .filter { it.isNotEmpty() }

    private fun readSmall(file: File): String? =
        file.takeIf { it.isFile && it.length() in 1..MAX_TEXT_BYTES }?.let { runCatching { it.readText() }.getOrNull() }
}
