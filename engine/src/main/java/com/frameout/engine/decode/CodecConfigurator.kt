package com.frameout.engine.decode

import android.media.MediaCodec
import android.media.MediaFormat

internal object CodecConfigurator {
    class Result(val codec: MediaCodec, val p010: Boolean)

    fun configure(first: MediaCodec, format: MediaFormat, name: String?, mime: String, allowP010: Boolean): Result {
        if (!(allowP010 && wantsP010(format) && supportsP010(first, mime))) {
            first.configure(format, null, null, 0)
            return Result(first, false)
        }
        val requested = MediaFormat(format).apply { setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAT_P010) }
        if (runCatching { first.configure(requested, null, null, 0) }.isSuccess) return Result(first, true)
        runCatching { first.release() }
        val retry = SystemSource.createDecoder(name, mime)
        try {
            retry.configure(format, null, null, 0)
        } catch (e: Exception) {
            runCatching { retry.release() }
            throw e
        }
        return Result(retry, false)
    }
}
