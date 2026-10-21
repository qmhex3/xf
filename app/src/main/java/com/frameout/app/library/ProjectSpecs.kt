package com.frameout.app.library

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.frameout.app.FrameOutApp
import com.frameout.codecpack.CodecPack
import com.frameout.codecpack.FfmpegBridge
import com.frameout.core.db.ProjectEntity
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
internal data class VideoSpecs(val shortSide: Int, val hdr: Boolean, val dolby: Boolean, val codec: String?, val fps: Float?)

private object SpecsStore {
    val memory = LruCache<Long, VideoSpecs>(256)
    val missed: MutableSet<String> = ConcurrentHashMap.newKeySet()
    fun prefs(context: Context) = context.applicationContext.getSharedPreferences("project_specs", Context.MODE_PRIVATE)
}
internal fun peekSpecs(project: ProjectEntity): VideoSpecs? = SpecsStore.memory.get(project.id)

internal suspend fun loadSpecs(context: Context, project: ProjectEntity): VideoSpecs? = withContext(Dispatchers.IO) {
    SpecsStore.memory.get(project.id)?.let { return@withContext it }
    readDisk(context, project)?.let { SpecsStore.memory.put(project.id, it); return@withContext it }
    val missKey = "${project.id}:${project.sourceUri}"
    var specs: VideoSpecs? = if (missKey in SpecsStore.missed) null else probeSource(context, Uri.parse(project.sourceUri))
    if (specs == null) SpecsStore.missed.add(missKey)
    if (specs?.fps == null) {
        val indexed = fpsFromIndex(context, project)
        if (indexed != null) specs = specs?.copy(fps = indexed) ?: VideoSpecs(0, false, false, null, indexed)
    }
    if (specs != null && specs.shortSide > 0) { writeDisk(context, project, specs); SpecsStore.memory.put(project.id, specs) }
    specs
}

private fun probeSource(context: Context, uri: Uri): VideoSpecs? = runCatching {
    val iso = runCatching { openChannel(context, uri) { IsoBmffProbe.probe(it) } }.getOrNull()
    val sys = runCatching { probeExtractor(context, uri) }.getOrNull()
    var shortSide = iso?.let { minOf(it.width, it.height) } ?: sys?.shortSide ?: 0
    var codec: String? = iso?.let { isoCodecLabel(it.sampleEntry) } ?: sys?.codec
    if (shortSide == 0 || codec == null) {
        runCatching { probeFfmpeg(context, uri) }.getOrNull()?.let { ff ->
            if (shortSide == 0) shortSide = ff.shortSide
            if (codec == null) codec = ff.codec
        }
    }
    if (shortSide == 0 && codec == null) return@runCatching null
    val transferHdr = iso?.transfer?.let { it == 16 || it == 18 } == true
    VideoSpecs(shortSide, transferHdr || sys?.hdr == true || iso?.dolby == true, iso?.dolby == true || sys?.dolby == true, codec, iso?.fps ?: sys?.fps)
}.getOrNull()

private fun <T> openChannel(context: Context, uri: Uri, block: (java.nio.channels.FileChannel) -> T?): T? {
    val pfd = (if (uri.scheme == "file") ParcelFileDescriptor.open(File(uri.path ?: return null), ParcelFileDescriptor.MODE_READ_ONLY)
    else context.contentResolver.openFileDescriptor(uri, "r")) ?: return null
    return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { block(it.channel) }
}

private data class Partial(val shortSide: Int, val hdr: Boolean, val dolby: Boolean, val codec: String?, val fps: Float?)

private fun probeExtractor(context: Context, uri: Uri): Partial? {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(context, uri, null)
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (!mime.startsWith("video/")) continue
            val w = intOrNull(f, MediaFormat.KEY_WIDTH) ?: 0
            val h = intOrNull(f, MediaFormat.KEY_HEIGHT) ?: 0
            val transfer = if (Build.VERSION.SDK_INT >= 24) intOrNull(f, MediaFormat.KEY_COLOR_TRANSFER) else null
            val dolby = mime == "video/dolby-vision"
            val hdr = dolby || transfer == MediaFormat.COLOR_TRANSFER_ST2084 || transfer == MediaFormat.COLOR_TRANSFER_HLG
            return Partial(if (w > 0 && h > 0) minOf(w, h) else 0, hdr, dolby, codecLabel(mime), floatOrNull(f, MediaFormat.KEY_FRAME_RATE))
        }
        return null
    } finally {
        extractor.release()
    }
}

private fun probeFfmpeg(context: Context, uri: Uri): Partial? {
    if (!CodecPack.isInstalled(context) || !CodecPack.load(context)) return null
    val pfd = (if (uri.scheme == "file") ParcelFileDescriptor.open(File(uri.path ?: return null), ParcelFileDescriptor.MODE_READ_ONLY)
    else context.contentResolver.openFileDescriptor(uri, "r")) ?: return null
    val path = "/proc/self/fd/${pfd.fd}"
    try {
        val parts = FfmpegBridge.nativeProbe(path).split('|')
        if (parts.size < 4) return null
        val w = parts[1].toIntOrNull() ?: 0
        val h = parts[2].toIntOrNull() ?: 0
        return Partial(if (w > 0 && h > 0) minOf(w, h) else 0, false, false, ffmpegCodecLabel(parts[0].removePrefix("video/")), null)
    } finally {
        runCatching { FfmpegBridge.nativeCloseSession(path) }
        runCatching { pfd.close() }
    }
}
private suspend fun fpsFromIndex(context: Context, project: ProjectEntity): Float? = runCatching {
    val dao = (context.applicationContext as FrameOutApp).container.videoIndex
    val n = dao.count(project.id)
    if (n < 3) return@runCatching null
    val first = dao.bySampleIndex(project.id, 0) ?: return@runCatching null
    val last = dao.bySampleIndex(project.id, n - 1) ?: return@runCatching null
    val span = last.ptsUs - first.ptsUs
    if (span <= 0) null else snapFps(((n - 1) * 1_000_000.0 / span).toFloat())
}.getOrNull()

private val COMMON_FPS = floatArrayOf(15f, 23.976f, 24f, 25f, 29.97f, 30f, 48f, 50f, 59.94f, 60f, 100f, 119.88f, 120f, 240f)
private fun snapFps(v: Float): Float = COMMON_FPS.minByOrNull { kotlin.math.abs(it - v) }?.takeIf { kotlin.math.abs(it - v) / it < 0.012f } ?: v

private fun readDisk(context: Context, project: ProjectEntity): VideoSpecs? = runCatching {
    val raw = SpecsStore.prefs(context).getString("p${project.id}", null) ?: return@runCatching null
    val p = raw.split('|')
    if (p.size < 6 || p[0] != project.sourceUri.hashCode().toString()) return@runCatching null
    VideoSpecs(p[1].toInt(), p[2] == "1", p[3] == "1", p[4].ifBlank { null }, p[5].toFloatOrNull())
}.getOrNull()

private fun writeDisk(context: Context, project: ProjectEntity, s: VideoSpecs) {
    SpecsStore.prefs(context).edit()
        .putString("p${project.id}", listOf(project.sourceUri.hashCode(), s.shortSide, if (s.hdr) 1 else 0, if (s.dolby) 1 else 0, s.codec.orEmpty().replace('|', ' '), s.fps ?: "").joinToString("|"))
        .apply()
}

private fun intOrNull(f: MediaFormat, key: String): Int? = if (f.containsKey(key)) runCatching { f.getInteger(key) }.getOrNull() else null
private fun floatOrNull(f: MediaFormat, key: String): Float? =
    if (!f.containsKey(key)) null else runCatching { f.getFloat(key) }.getOrNull() ?: runCatching { f.getInteger(key).toFloat() }.getOrNull()

internal fun resolutionLabel(shortSide: Int): String = when {
    shortSide >= 4320 -> "8K"
    shortSide >= 2160 -> "4K"
    shortSide >= 1440 -> "2K"
    shortSide >= 1080 -> "1080p"
    shortSide >= 720 -> "720p HD"
    else -> "${shortSide}p"
}
internal fun specsHeadline(s: VideoSpecs): String? {
    if (s.shortSide <= 0) return null
    val base = resolutionLabel(s.shortSide)
    return when {
        s.dolby -> "$base DV"
        s.hdr -> "$base HDR"
        else -> base
    }
}

internal fun codecLabel(mime: String): String = when (mime.lowercase(Locale.US)) {
    "video/avc" -> "H.264"
    "video/hevc" -> "HEVC"
    "video/av01" -> "AV1"
    "video/x-vnd.on2.vp9" -> "VP9"
    "video/x-vnd.on2.vp8" -> "VP8"
    "video/mp4v-es" -> "MPEG-4"
    "video/mpeg2" -> "MPEG-2"
    "video/3gpp" -> "H.263"
    "video/dolby-vision" -> "Dolby Vision"
    else -> mime.substringAfter('/').removePrefix("x-").uppercase(Locale.US)
}
internal fun isoCodecLabel(fourcc: String): String? = when (fourcc.lowercase(Locale.US)) {
    "avc1", "avc3", "dvav", "dva1" -> "H.264"
    "hvc1", "hev1", "dvh1", "dvhe" -> "HEVC"
    "av01", "dav1" -> "AV1"
    "vp09" -> "VP9"
    "vp08" -> "VP8"
    "mp4v" -> "MPEG-4"
    "s263", "h263" -> "H.263"
    "apco" -> "ProRes 422 Proxy"
    "apcs" -> "ProRes 422 LT"
    "apcn" -> "ProRes 422"
    "apch" -> "ProRes 422 HQ"
    "ap4h" -> "ProRes 4444"
    "ap4x" -> "ProRes 4444 XQ"
    "aprn" -> "ProRes RAW"
    "aprh" -> "ProRes RAW HQ"
    "avdn" -> "DNxHD"
    "avdh" -> "DNxHR"
    "mjpa", "mjpb", "jpeg" -> "MJPEG"
    "mp2v", "m2v1", "mx3p", "mx4p", "mx5p", "mx3n", "mx4n", "mx5n", "xdv2", "xdv3", "xdvb", "xdve" -> "MPEG-2"
    else -> null
}

internal fun ffmpegCodecLabel(name: String): String = when (name.lowercase(Locale.US)) {
    "h264" -> "H.264"
    "hevc" -> "HEVC"
    "av1" -> "AV1"
    "vp9" -> "VP9"
    "vp8" -> "VP8"
    "vvc" -> "VVC"
    "mpeg4" -> "MPEG-4"
    "mpeg2video" -> "MPEG-2"
    "mpeg1video" -> "MPEG-1"
    "prores" -> "ProRes"
    "dnxhd" -> "DNxHD"
    "mjpeg" -> "MJPEG"
    "rv40" -> "RealVideo 4"
    "rv30" -> "RealVideo 3"
    "rv20", "rv10" -> "RealVideo"
    "wmv1", "wmv2", "wmv3" -> "WMV"
    "vc1" -> "VC-1"
    "theora" -> "Theora"
    "h263" -> "H.263"
    "msmpeg4v3", "msmpeg4v2", "msmpeg4v1" -> "MS MPEG-4"
    "ffv1" -> "FFV1"
    "hap" -> "HAP"
    "cinepak" -> "Cinepak"
    else -> name.uppercase(Locale.US)
}

internal fun fpsLabel(fps: Float): String {
    val rounded = Math.round(fps)
    return if (kotlin.math.abs(fps - rounded) < 0.01f) "$rounded fps" else String.format(Locale.US, "%.2f fps", fps)
}
