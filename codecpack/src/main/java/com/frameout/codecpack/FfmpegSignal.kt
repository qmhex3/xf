package com.frameout.codecpack

class FfmpegSignalFrame(val width: Int, val height: Int, val rgb: ShortArray, val primaries: Int, val transfer: Int)

object FfmpegSignal {
    private const val META_SIZE = 4

    fun request(enabled: Boolean) {
        if (!FfmpegBridge.isAvailable) return
        runCatching { FfmpegBridge.nativeSetSignalEnabled(enabled) }
    }

    fun take(path: String, width: Int, height: Int): FfmpegSignalFrame? {
        if (!FfmpegBridge.isAvailable || width <= 0 || height <= 0) return null
        val count = width.toLong() * height.toLong() * 3L
        if (count > Int.MAX_VALUE) return null
        return try {
            val meta = IntArray(META_SIZE)
            if (FfmpegBridge.nativeLastSignal(path, null, meta) != 0) return null
            if (meta[0] != width || meta[1] != height) return null
            val rgb = ShortArray(count.toInt())
            if (FfmpegBridge.nativeLastSignal(path, rgb, meta) != 0) return null
            FfmpegSignalFrame(meta[0], meta[1], rgb, meta[2], meta[3])
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: UnsatisfiedLinkError) {
            null
        }
    }
}
