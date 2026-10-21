package com.frameout.codecpack

object FfmpegBridge {
    @Volatile private var loaded = false
    @Volatile private var loadError: String? = null

    val isAvailable: Boolean get() = loaded

    val error: String? get() = loadError

    fun tryLoad(libraryPath: String? = null): Boolean {
        if (loaded) return true
        return try {
            if (libraryPath != null) {
                System.load(libraryPath)
            } else {
                System.loadLibrary("frameout_ffmpeg")
            }
            loaded = nativeInit() == 0
            if (!loaded) loadError = "nativeInit failed"
            loaded
        } catch (e: UnsatisfiedLinkError) {
            loadError = e.message
            false
        } catch (e: Exception) {
            loadError = e.message
            false
        }
    }

    external fun nativeInit(): Int

    external fun nativeLastError(path: String): Int

    fun lastFailure(path: String, fallback: MediaFailure = MediaFailure.Unknown): MediaFailure {
        val code = try {
            nativeLastError(path)
        } catch (e: UnsatisfiedLinkError) {
            return fallback
        }
        return MediaFailure.fromCode(code).takeIf { it != MediaFailure.None } ?: fallback
    }

    external fun nativeProbe(path: String): String

    external fun nativeFrameCount(path: String): Int

    external fun nativeDecodeFrame(
        path: String,
        index: Int,
        outRgba: java.nio.ByteBuffer,
        width: Int,
        height: Int,
        ptsOut: LongArray,
        keyframeOut: BooleanArray,
        nearestKeyframeIndexOut: IntArray,
        nearestKeyframePtsOut: LongArray
    ): Int


    external fun nativeDecodeFrameFrom(
        path: String,
        index: Int,
        hintIndex: Int,
        hintPtsUs: Long,
        outRgba: java.nio.ByteBuffer,
        width: Int,
        height: Int,
        ptsOut: LongArray,
        keyframeOut: BooleanArray,
        nearestKeyframeIndexOut: IntArray,
        nearestKeyframePtsOut: LongArray
    ): Int

    external fun nativeDecodeFrameToBitmap(
        path: String,
        index: Int,
        hintIndex: Int,
        hintPtsUs: Long,
        bitmap: android.graphics.Bitmap,
        ptsOut: LongArray,
        keyframeOut: BooleanArray,
        nearestKeyframeIndexOut: IntArray,
        nearestKeyframePtsOut: LongArray
    ): Int

    external fun nativeFrameIndex(path: String): LongArray?

    external fun nativeFrameIndexPrefix(path: String, maxPackets: Int, completeOut: BooleanArray): LongArray?

    external fun nativeFrameIndexCancellable(path: String, token: Long): LongArray?

    external fun nativeSetSessionTable(path: String, table: LongArray): Int

    external fun nativeSetIndexCancel(token: Long, cancelled: Boolean)

    external fun nativeReleaseIndexCancel(token: Long)

    external fun nativeWidth(path: String): Int

    external fun nativeHeight(path: String): Int
    external fun nativeSeekSessionToKeyframe(path: String, keyframeIndex: Int, keyframePtsUs: Long): Int

    external fun nativeCloseSession(path: String)

    external fun nativeCancelSession(path: String)

    external fun nativeSetGainmapEnabled(enabled: Boolean)

    external fun nativeLastGainmap(path: String, outGain: java.nio.ByteBuffer, meta: FloatArray): Int

    external fun nativeSetSignalEnabled(enabled: Boolean)

    external fun nativeLastSignal(path: String, outRgb: ShortArray?, meta: IntArray): Int
}
