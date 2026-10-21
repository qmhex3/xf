package com.frameout.engine.decode

import com.frameout.core.util.SmoothScale
import com.frameout.core.util.logged
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.media.MediaMetadataRetriever
import com.frameout.codecpack.CodecPack
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FormatProbe
import com.frameout.codecpack.FfmpegBridge
import com.frameout.codecpack.FormatInfo
import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureException

data class FfmpegDecodedFrame(val ptsUs: Long, val bitmap: Bitmap, val isKeyframe: Boolean, val nearestKeyframeIndex: Int = -1, val nearestKeyframePtsUs: Long = -1L)

class DecodeRouter(private val context: Context, private val cancelled: () -> Boolean = { false }) {
    var reuseCopy: Boolean
        get() = copier.reuse
        set(value) { copier.reuse = value }
    var retainCopy = false
    var forceFfmpeg = false
    var previewMaxSide = 0
    private val system = FrameDecoder(context)
    private val gif = GifFrameDecoder(context)
    private val apng = ApngFrameDecoder(context)
    private val webp = WebpFrameDecoder(context)
    private var kind: FormatKind = FormatKind.System
    private var pathForNative: String? = null
    private var nativeDescriptor: ParcelFileDescriptor? = null
    private val copier = SourceCopier(context, cancelled)
    private var proxyThread: android.os.HandlerThread? = null
    private var nativeWidthValue = 0
    private var nativeHeightValue = 0
    private var rgbaBuffer: java.nio.ByteBuffer? = null
    private var lastFfmpegIndex = -1
    private var reducedNoted = false
    private var directBitmapBroken = false
    private var fallbackUri: Uri? = null
    private var fallbackRetriever: MediaMetadataRetriever? = null

    fun open(uri: Uri) {
        close()
        val info = FormatProbe.probe(context, uri)
        kind = if (forceFfmpeg && (info.kind == FormatKind.System || info.kind == FormatKind.AnimatedImage)) FormatKind.NeedsFfmpeg else info.kind
        fallbackUri = uri
        when (kind) {
            FormatKind.Gif -> gif.open(uri)
            FormatKind.Apng -> apng.open(uri)
            FormatKind.AnimatedWebp -> webp.open(uri)
            FormatKind.System, FormatKind.AnimatedImage -> system.open(uri)
            FormatKind.NeedsFfmpeg -> openFfmpeg(uri, info)
            FormatKind.Unsupported -> throw MediaFailureException(
                info.failure.takeIf { it != MediaFailure.None } ?: MediaFailure.UnsupportedContainer,
                info.message.ifBlank { "Unsupported format" }
            )
        }
    }

    private fun openFfmpeg(uri: Uri, info: FormatInfo) {
        if (!CodecPack.load(context)) {
            throw MediaFailureException(MediaFailure.CodecPackMissing, info.message.ifBlank { "Codec pack required for ${info.container}" })
        }
        var path = uriToPath(uri) ?: throw MediaFailureException(MediaFailure.IoError, "FFmpeg needs local path")
        pathForNative = path
        var probedWidth = FfmpegBridge.nativeWidth(path)
        var probedHeight = FfmpegBridge.nativeHeight(path)
        if ((probedWidth <= 0 || probedHeight <= 0) && proxyThread != null) {
            runCatching { FfmpegBridge.nativeCloseSession(path) }
            releaseProxy()
            val plain = context.contentResolver.openFileDescriptor(uri, "r")
            val copied = plain?.let { copier.copy(uri, it) }
            if (copied != null) {
                path = copied
                pathForNative = path
                probedWidth = FfmpegBridge.nativeWidth(path)
                probedHeight = FfmpegBridge.nativeHeight(path)
            }
        }
        if (probedWidth <= 0 || probedHeight <= 0) {
            pathForNative = null
            throw MediaFailureException(FfmpegBridge.lastFailure(path, MediaFailure.CorruptData), "FFmpeg could not read video dimensions for ${info.container}")
        }
        nativeWidthValue = probedWidth
        nativeHeightValue = probedHeight
    }

    fun lastFfmpegFailure(): MediaFailure {
        val path = pathForNative ?: return MediaFailure.CorruptData
        return FfmpegBridge.lastFailure(path, MediaFailure.CorruptData)
    }

    fun seekToKeyframe(timeUs: Long) {
        when (kind) {
            FormatKind.System, FormatKind.AnimatedImage -> system.seekToKeyframe(timeUs)
            else -> Unit
        }
    }

    fun decodeNextBitmap(): Pair<Long, Bitmap>? {
        return when (kind) {
            FormatKind.System, FormatKind.AnimatedImage -> system.decodeNextBitmap()
            FormatKind.Gif, FormatKind.Apng, FormatKind.AnimatedWebp -> null
            FormatKind.NeedsFfmpeg -> null
            FormatKind.Unsupported -> null
        }
    }

    fun decodeAnimatedFrame(index: Int): Pair<Long, Bitmap>? = animatedSource()?.decodeFrame(index)
    fun animatedFrameCount(): Int = animatedSource()?.frameCount() ?: 0
    private fun animatedSource(): AnimatedFrameSource? = when (kind) {
        FormatKind.Apng -> apng
        FormatKind.AnimatedWebp -> webp
        else -> gif
    }

    fun seekFfmpegToKeyframe(keyframeIndex: Int, keyframePtsUs: Long): Boolean {
        val path = pathForNative ?: return false
        return FfmpegBridge.nativeSeekSessionToKeyframe(path, keyframeIndex, keyframePtsUs) == 0
    }

    fun wantsKeyframeHint(index: Int): Boolean =
        kind == FormatKind.NeedsFfmpeg && KeyframeHintPolicy.needed(lastFfmpegIndex, index)

    fun decodeFfmpegFrame(index: Int, hintIndex: Int = -1, hintPtsUs: Long = -1L): FfmpegDecodedFrame? {
        val path = pathForNative ?: return null
        val (w, h) = frameSize() ?: return null
        if (!reducedNoted && previewMaxSide == 0 && (w < nativeWidthValue || h < nativeHeightValue)) {
            reducedNoted = true
            DecodeQuality.markReduced("codec pack frame ${nativeWidthValue}x${nativeHeightValue} scaled to ${w}x${h}", nativeWidthValue, nativeHeightValue, w, h)
        }
        val pts = LongArray(1)
        val keyframe = BooleanArray(1)
        val nearestIndex = IntArray(1)
        val nearestPts = LongArray(1)
        com.frameout.codecpack.FfmpegGainmap.request(com.frameout.core.hdr.HdrSupport.enabled(context))
        if (!directBitmapBroken) {
            val target = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val direct = FfmpegBridge.nativeDecodeFrameToBitmap(path, index, hintIndex, hintPtsUs, target, pts, keyframe, nearestIndex, nearestPts)
            if (direct != DIRECT_UNSUPPORTED) return finishFfmpeg(path, index, direct, target, pts, keyframe, nearestIndex, nearestPts)
            target.recycle()
            directBitmapBroken = true
        }
        val bytes = w.toLong() * h.toLong() * 4L
        val rgba = rgbaBuffer?.takeIf { it.capacity().toLong() == bytes } ?: java.nio.ByteBuffer.allocateDirect(bytes.toInt()).also { rgbaBuffer = it }
        val rc = FfmpegBridge.nativeDecodeFrameFrom(path, index, hintIndex, hintPtsUs, rgba, w, h, pts, keyframe, nearestIndex, nearestPts)
        val bmp = if (rc == 0) Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { rgba.rewind(); it.copyPixelsFromBuffer(rgba) } else null
        return finishFfmpeg(path, index, rc, bmp, pts, keyframe, nearestIndex, nearestPts)
    }

    private fun frameSize(): Pair<Int, Int>? {
        val fit = com.frameout.codecpack.FrameBudget.fit(nativeWidthValue.coerceAtLeast(1), nativeHeightValue.coerceAtLeast(1)) ?: return null
        val longest = maxOf(fit.first, fit.second)
        if (previewMaxSide <= 0 || longest <= previewMaxSide) return fit
        val k = previewMaxSide.toFloat() / longest
        return (fit.first * k).toInt().coerceAtLeast(1) to (fit.second * k).toInt().coerceAtLeast(1)
    }

    private fun finishFfmpeg(path: String, index: Int, rc: Int, bitmap: Bitmap?, pts: LongArray, keyframe: BooleanArray, nearestIndex: IntArray, nearestPts: LongArray): FfmpegDecodedFrame? {
        if (rc == -5 || rc == 0) lastFfmpegIndex = index else lastFfmpegIndex = -1
        if (rc == -5) {
            bitmap?.recycle()
            return fallbackFrame(index, pts[0])
        }
        if (rc != 0 || bitmap == null) {
            bitmap?.recycle()
            return null
        }
        com.frameout.codecpack.FfmpegGainmap.attach(path, bitmap)
        return FfmpegDecodedFrame(pts[0].coerceAtLeast(0L), bitmap, keyframe[0], nearestIndex[0], nearestPts[0])
    }

    private fun fallbackFrame(index: Int, ptsUs: Long): FfmpegDecodedFrame? = runCatching {
        val uri = fallbackUri ?: return null
        val retriever = fallbackRetriever ?: MediaMetadataRetriever().also { it.setDataSource(context, uri); fallbackRetriever = it }
        val bitmap = if (ptsUs >= 0L) retriever.getFrameAtTime(ptsUs, MediaMetadataRetriever.OPTION_CLOSEST) else null
        if (bitmap == null) null else FfmpegDecodedFrame(ptsUs.coerceAtLeast(0L), scaleFallback(FallbackColor.normalize(bitmap)), false)
    }.logged("DecodeRouter").getOrNull()

    private fun scaleFallback(bitmap: Bitmap): Bitmap {
        val max = 2048
        if (bitmap.width <= max && bitmap.height <= max) return bitmap
        val factor = max.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaled = SmoothScale.scaled(bitmap, (bitmap.width * factor).toInt(), (bitmap.height * factor).toInt(), true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    fun nativePath(): String? = pathForNative
    fun ffmpegWidth(): Int = nativeWidthValue
    fun ffmpegHeight(): Int = nativeHeightValue
    fun ffmpegFrameCount(): Int = pathForNative?.let { FfmpegBridge.nativeFrameCount(it).coerceAtLeast(0) } ?: 0

    fun currentKind(): FormatKind = kind
    fun cancelNative() {
        pathForNative?.let { path -> runCatching { FfmpegBridge.nativeCancelSession(path) } }
    }

    fun close() {
        pathForNative?.let { path -> runCatching { FfmpegBridge.nativeCloseSession(path) } }
        system.close()
        gif.close()
        apng.close()
        webp.close()
        pathForNative = null
        nativeWidthValue = 0
        nativeHeightValue = 0
        rgbaBuffer = null
        fallbackUri = null
        lastFfmpegIndex = -1
        reducedNoted = false
        runCatching { fallbackRetriever?.release() }
        fallbackRetriever = null
        releaseProxy()
        copier.release(retainCopy)
        kind = FormatKind.System
    }

    private fun uriToPath(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        if (isSeekableRegularFile(descriptor)) {
            nativeDescriptor = descriptor
            return "/proc/self/fd/${descriptor.fd}"
        }
        return openStreaming(uri, descriptor) ?: copier.copy(uri, descriptor)
    }

    private fun openStreaming(uri: Uri, descriptor: ParcelFileDescriptor): String? {
        if (android.os.Build.VERSION.SDK_INT < 26) return null
        val size = querySize(uri).takeIf { it > 0L } ?: descriptor.statSize.takeIf { it > 0L } ?: return null
        val thread = android.os.HandlerThread("frameout-source").also { it.start() }
        return try {
            val manager = context.getSystemService(android.os.storage.StorageManager::class.java)
            val proxy = manager.openProxyFileDescriptor(
                ParcelFileDescriptor.MODE_READ_ONLY,
                StreamingProxySource(context, uri, size),
                android.os.Handler(thread.looper)
            )
            runCatching { descriptor.close() }
            nativeDescriptor = proxy
            proxyThread = thread
            "/proc/self/fd/${proxy.fd}"
        } catch (error: Throwable) {
            thread.quitSafely()
            null
        }
    }

    private fun querySize(uri: Uri): Long = runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use {
            if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else -1L
        } ?: -1L
    }.getOrDefault(-1L)

    private fun releaseProxy() {
        runCatching { nativeDescriptor?.close() }
        nativeDescriptor = null
        proxyThread?.quitSafely()
        proxyThread = null
    }

    private fun isSeekableRegularFile(descriptor: ParcelFileDescriptor): Boolean = try {
        val stat = android.system.Os.fstat(descriptor.fileDescriptor)
        android.system.OsConstants.S_ISREG(stat.st_mode) &&
            android.system.Os.lseek(descriptor.fileDescriptor, 0L, android.system.OsConstants.SEEK_CUR) >= 0L
    } catch (error: Throwable) {
        false
    }

    private companion object {
        const val DIRECT_UNSUPPORTED = -102
    }
}
