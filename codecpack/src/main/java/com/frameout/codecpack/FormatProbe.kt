package com.frameout.codecpack

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.FileNotFoundException
import java.io.IOException

object FormatProbe {
    private const val WEBP_HEAD_BYTES = 32
    private val ffmpegExt: Set<String> = ContainerSniffer.videoExtensions

    private const val CACHE_TTL_MS = 60_000L
    private const val CACHE_SIZE = 16

    private class Cached(val info: FormatInfo, val codecPack: Boolean, val at: Long)

    private val cache = object : LinkedHashMap<String, Cached>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Cached>?): Boolean = size > CACHE_SIZE
    }

    fun probe(context: Context, uri: Uri): FormatInfo {
        val key = uri.toString()
        val codecPack = CodecPack.isInstalled(context)
        val now = System.currentTimeMillis()
        synchronized(cache) {
            cache[key]?.let { if (it.codecPack == codecPack && now - it.at in 0..CACHE_TTL_MS) return it.info }
        }
        val info = probeUncached(context, uri)
        if (info.kind != FormatKind.Unsupported) synchronized(cache) { cache[key] = Cached(info, codecPack, now) }
        return info
    }

    fun forget(uri: Uri) {
        synchronized(cache) { cache.remove(uri.toString()) }
    }

    private fun probeUncached(context: Context, uri: Uri): FormatInfo {
        val source = sourceInfo(context, uri)
        val ext = source.name.substringAfterLast('.', "").lowercase()
        val mime = source.mime.lowercase()
        accessFailure(context, uri)?.let { return FormatInfo("unknown", ext.ifBlank { "unknown" }, FormatKind.Unsupported, it.name, it) }
        if (mime == "image/gif" || ext == "gif") return FormatInfo("image/gif", "gif", FormatKind.Gif)
        if (ext == "apng" || ext == "png" || mime == "image/apng" || mime == "image/png") {
            if (isApng(context, uri)) return FormatInfo("image/apng", "apng", FormatKind.Apng)
            if (ext == "apng" || mime == "image/apng") return FormatInfo("image/apng", "apng", FormatKind.Unsupported, "Static image", MediaFailure.NotAnimated)
        }
        if (ext == "avif" || mime == "image/avif") return FormatInfo("image/avif", "avif", FormatKind.NeedsFfmpeg, "AVIF needs codec pack")
        if (ext == "webp" || mime == "image/webp") {
            return if (isAnimatedWebp(context, uri)) FormatInfo("image/webp", "webp", FormatKind.AnimatedWebp)
            else FormatInfo(mime.ifBlank { "image/$ext" }, ext.ifBlank { "image" }, FormatKind.Unsupported, "Static image", MediaFailure.NotAnimated)
        }
        probeExtractor(context, uri, ext)?.let { return it }
        if (ext in ffmpegExt) return FormatInfo("video/$ext", ext, FormatKind.NeedsFfmpeg, "Container $ext needs codec pack")
        probeRetriever(context, uri, ext)?.let { return it }
        val sniffed = sniff(context, uri)
        if (sniffed != null && sniffed.video) return FormatInfo("video/${sniffed.container}", sniffed.container, FormatKind.NeedsFfmpeg, "Container ${sniffed.container} needs codec pack")
        if (sniffed == null && mime in OPEN_MIMES && CodecPack.isInstalled(context)) return FormatInfo("video/*", ext.ifBlank { "unknown" }, FormatKind.NeedsFfmpeg, "Unknown container, trying codec pack")
        return FormatInfo("unknown", ext.ifBlank { "unknown" }, FormatKind.Unsupported, "Unsupported container: ${ext.ifBlank { sniffed?.container ?: "unknown" }}", MediaFailure.UnsupportedContainer)
    }

    private fun sourceInfo(context: Context, uri: Uri): SourceInfo {
        var name = uri.lastPathSegment.orEmpty()
        var mime = context.contentResolver.getType(uri).orEmpty()
        if (uri.scheme == "content") runCatching { queryName(context, uri)?.let { name = it } }
        if (mime.isBlank()) MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', ""))?.let { mime = it }
        return SourceInfo(name, mime)
    }

    private fun queryName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)).orEmpty() else null
        }
    }.getOrNull()?.ifBlank { null }

    private fun isApng(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.buffered()?.use { ApngParser.isAnimated(it) } ?: false
    }.getOrDefault(false)

    private fun isAnimatedWebp(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val head = ByteArray(WEBP_HEAD_BYTES)
            var read = 0
            while (read < head.size) {
                val n = input.read(head, read, head.size - read)
                if (n < 0) break
                read += n
            }
            WebpAnimParser.isAnimatedHeader(head, read)
        } ?: false
    }.getOrDefault(false)

    private fun probeExtractor(context: Context, uri: Uri, ext: String): FormatInfo? = runCatching {
        val ex = MediaExtractor()
        try {
            ex.setDataSource(context, uri, null)
            val track = VideoTrackSelector.select(ex)
            if (track < 0) return@runCatching null
            val format = ex.getTrackFormat(track)
            val mime = format.getString(android.media.MediaFormat.KEY_MIME).orEmpty()
            if (mime.isEmpty()) return@runCatching null
            if (isProtected(ex)) return@runCatching FormatInfo(mime, ext.ifBlank { "media" }, FormatKind.Unsupported, "Protected content", MediaFailure.Drm)
            if (DolbyVision.isProfile5(format) && CodecPack.isInstalled(context)) {
                return@runCatching FormatInfo(mime, ext.ifBlank { "media" }, FormatKind.NeedsFfmpeg, "Dolby Vision profile 5 decodes with the codec pack")
            }
            if (!DecoderSupport.supports(format)) {
                val message = if (mime.equals("video/av01", ignoreCase = true)) "AV1 has no hardware decoder on this device, using codec pack" else "No decoder for $mime on this device"
                return@runCatching FormatInfo(mime, ext.ifBlank { "media" }, FormatKind.NeedsFfmpeg, message)
            }
            FormatInfo(mime, ext.ifBlank { "media" }, FormatKind.System)
        } finally { ex.release() }
    }.getOrNull()

    private fun isProtected(extractor: MediaExtractor): Boolean = runCatching { extractor.psshInfo?.isNotEmpty() == true }.getOrDefault(false)

    private fun accessFailure(context: Context, uri: Uri): MediaFailure? = try {
        val stream = context.contentResolver.openInputStream(uri)
        if (stream == null) MediaFailure.SourceMissing else stream.use { if (it.read() < 0) MediaFailure.CorruptData else null }
    } catch (e: FileNotFoundException) {
        MediaFailure.SourceMissing
    } catch (e: SecurityException) {
        MediaFailure.PermissionDenied
    } catch (e: IOException) {
        null
    } catch (e: RuntimeException) {
        null
    }

    private fun sniff(context: Context, uri: Uri): Sniffed? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val head = ByteArray(ContainerSniffer.HEAD_BYTES)
            var read = 0
            while (read < head.size) {
                val n = input.read(head, read, head.size - read)
                if (n < 0) break
                read += n
            }
            ContainerSniffer.sniff(head, read)
        }
    }.getOrNull()

    private fun probeRetriever(context: Context, uri: Uri, ext: String): FormatInfo? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            if (hasVideo == "yes") FormatInfo("video/*", ext, FormatKind.System) else null
        } finally { retriever.release() }
    }.getOrNull()

    private val OPEN_MIMES = setOf("", "application/octet-stream", "video/*", "application/x-matroska", "application/mxf", "application/vnd.rn-realmedia", "application/vnd.rn-realmedia-vbr", "application/x-troff-msvideo")

    private data class SourceInfo(val name: String, val mime: String)
}
