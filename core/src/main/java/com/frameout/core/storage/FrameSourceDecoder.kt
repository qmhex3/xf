package com.frameout.core.storage

import com.frameout.core.util.logged
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import com.frameout.codecpack.CodecPack
import com.frameout.codecpack.FfmpegBridge
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FfmpegSignal
import com.frameout.codecpack.FormatProbe
import com.frameout.codecpack.FrameBudget
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.VideoIndexDao
import com.frameout.core.hdr.FfmpegSignalLink
import com.frameout.core.hdr.HdrSignalRegistry
import java.io.File
import java.nio.ByteBuffer

class FrameSourceDecoder(private val context: Context, private val videoIndexDao: VideoIndexDao? = null) {
    private class Session(val key: String, val descriptor: ParcelFileDescriptor?, val path: String, val pts: LongArray?)

    private var session: Session? = null
    private var infoKey: String? = null
    private var cachedInfo: com.frameout.codecpack.FormatInfo? = null
    private var retriever: MediaMetadataRetriever? = null
    private var retrieverKey: String? = null

    @Synchronized
    fun decode(frame: FrameEntity, source: Uri): Bitmap? {
        val info = infoFor(source)
        if (AnimatedFrameLookup.handles(info.kind)) {
            closeSession()
            AnimatedFrameLookup.provider?.decodeAtPts(source, info.kind, frame.ptsUs)?.let { return it }
        }
        if (info.kind == FormatKind.NeedsFfmpeg && CodecPack.load(context)) return decodeFfmpeg(frame, source)
        closeSession()
        return decodeRetriever(source, frame.ptsUs)
    }

    @Synchronized
    fun decodeExact(frame: FrameEntity, source: Uri): Bitmap? {
        val info = infoFor(source)
        val usable = info.kind == FormatKind.NeedsFfmpeg || info.kind == FormatKind.System
        if (!usable || !CodecPack.load(context)) return null
        FfmpegSignal.request(true)
        try {
            val bitmap = decodeFfmpeg(frame, source, true) ?: return null
            if (HdrSignalRegistry.has(bitmap)) return bitmap
            bitmap.recycle()
            return null
        } finally {
            FfmpegSignal.request(false)
        }
    }

    @Synchronized
    fun close() {
        closeSession()
        releaseRetriever()
        infoKey = null
        cachedInfo = null
    }

    private fun infoFor(source: Uri): com.frameout.codecpack.FormatInfo {
        val key = source.toString()
        cachedInfo?.takeIf { infoKey == key }?.let { return it }
        val probed = FormatProbe.probe(context, source)
        infoKey = key
        cachedInfo = probed
        return probed
    }

    private fun releaseRetriever() {
        runCatching { retriever?.release() }
        retriever = null
        retrieverKey = null
    }

    private fun retrieverFor(source: Uri): MediaMetadataRetriever {
        val key = source.toString()
        retriever?.takeIf { retrieverKey == key }?.let { return it }
        releaseRetriever()
        val fresh = MediaMetadataRetriever()
        try {
            fresh.setDataSource(context, source)
        } catch (e: Exception) {
            runCatching { fresh.release() }
            throw e
        }
        retriever = fresh
        retrieverKey = key
        return fresh
    }

    private fun closeSession() {
        val current = session ?: return
        session = null
        runCatching { FfmpegBridge.nativeCloseSession(current.path) }
        runCatching { current.descriptor?.close() }
    }

    private fun openSession(source: Uri): Session? {
        val key = source.toString()
        session?.takeIf { it.key == key }?.let { return it }
        closeSession()
        val descriptor = descriptor(source) ?: return null
        val path = if (source.scheme == "file") source.path.also { runCatching { descriptor.close() } } else "/proc/self/fd/${descriptor.fd}"
        if (path == null) {
            runCatching { descriptor.close() }
            return null
        }
        val owned = if (source.scheme == "file") null else descriptor
        val opened = Session(key, owned, path, FfmpegBridge.nativeFrameIndex(path)?.let(::ptsOf))
        session = opened
        return opened
    }

    private fun ptsOf(index: LongArray): LongArray {
        val count = index.size / 4
        return LongArray(count) { index[it * 4] }
    }

    private fun decodeFfmpeg(frame: FrameEntity, source: Uri, exact: Boolean = false): Bitmap? = runCatching {
        val active = openSession(source) ?: return null
        val path = active.path
        val stored = storedSample(frame)
        val best = stored?.sampleIndex ?: findBestSample(active.pts, frame.ptsUs)
        if (best < 0) return null
        val nearestIndex = stored?.nearestKeyframeIndex ?: -1
        val nearestPts = stored?.nearestKeyframePtsUs ?: -1L
        if (nearestIndex >= 0 && nearestPts >= 0) FfmpegBridge.nativeSeekSessionToKeyframe(path, nearestIndex, nearestPts)
        val sourceW = FfmpegBridge.nativeWidth(path)
        val sourceH = FfmpegBridge.nativeHeight(path)
        if (sourceW <= 0 || sourceH <= 0) return null
        val (width, height) = (if (exact) FrameBudget.fitExact(sourceW, sourceH) else FrameBudget.fit(sourceW, sourceH)) ?: return null
        val rgba = ByteBuffer.allocateDirect(width * height * 4)
        val pts = LongArray(1)
        val key = BooleanArray(1)
        val nearestIndexOut = IntArray(1)
        val nearestPtsOut = LongArray(1)
        com.frameout.codecpack.FfmpegGainmap.request(com.frameout.core.hdr.HdrSupport.enabled(context))
        if (FfmpegBridge.nativeDecodeFrame(path, best, rgba, width, height, pts, key, nearestIndexOut, nearestPtsOut) != 0) return null
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            rgba.rewind()
            it.copyPixelsFromBuffer(rgba)
            com.frameout.codecpack.FfmpegGainmap.attach(path, it)
            if (exact) FfmpegSignalLink.attach(path, it)
        }
    }.logged("FrameSourceDecoder").getOrElse {
        closeSession()
        null
    }

    private fun storedSample(frame: FrameEntity): com.frameout.core.db.VideoIndexEntity? {
        val dao = videoIndexDao ?: return null
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return null
        return kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) { dao.bySampleIndex(frame.projectId, frame.index) }
    }

    private fun findBestSample(pts: LongArray?, ptsUs: Long): Int {
        if (pts == null || pts.isEmpty()) return -1
        var best = -1
        var distance = Long.MAX_VALUE
        for (i in pts.indices) {
            val current = kotlin.math.abs(pts[i] - ptsUs)
            if (current < distance) { distance = current; best = i }
        }
        return best
    }

    private fun decodeRetriever(source: Uri, ptsUs: Long): Bitmap? = runCatching {
        val active = retrieverFor(source)
        if (Build.VERSION.SDK_INT >= 27) return@runCatching active.getScaledFrameAtTime(ptsUs, MediaMetadataRetriever.OPTION_CLOSEST, 2048, 2048)
        val width = active.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: return@runCatching null
        val height = active.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: return@runCatching null
        if (width.toLong() * height.toLong() > 24_000_000L) return@runCatching null
        active.getFrameAtTime(ptsUs, MediaMetadataRetriever.OPTION_CLOSEST)
    }.logged("FrameSourceDecoder").getOrElse {
        releaseRetriever()
        null
    }

    private fun descriptor(uri: Uri): ParcelFileDescriptor? {
        if (uri.scheme == "file") return ParcelFileDescriptor.open(File(uri.path ?: return null), ParcelFileDescriptor.MODE_READ_ONLY)
        return context.contentResolver.openFileDescriptor(uri, "r")
    }
}
