package com.frameout.codecpack

import android.graphics.Bitmap
import android.graphics.Gainmap
import android.os.Build
import java.nio.ByteBuffer

object FfmpegGainmap {
    fun request(enabled: Boolean) {
        if (!FfmpegBridge.isAvailable) return
        runCatching { FfmpegBridge.nativeSetGainmapEnabled(enabled && Build.VERSION.SDK_INT >= 34) }
    }

    fun attach(path: String, bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT < 34 || !FfmpegBridge.isAvailable) return
        runCatching {
            val meta = FloatArray(META_SIZE)
            val probe = ByteBuffer.allocateDirect(bitmap.width * bitmap.height * 3)
            if (FfmpegBridge.nativeLastGainmap(path, probe, meta) != 0) return
            val gw = meta[0].toInt()
            val gh = meta[1].toInt()
            if (gw <= 0 || gh <= 0 || gw * gh * 3 > probe.capacity()) return
            val pixels = IntArray(gw * gh)
            for (i in pixels.indices) {
                val r = probe.get(i * 3).toInt() and 0xFF
                val g = probe.get(i * 3 + 1).toInt() and 0xFF
                val b = probe.get(i * 3 + 2).toInt() and 0xFF
                pixels[i] = -0x1000000 or (r shl 16) or (g shl 8) or b
            }
            val map = Bitmap.createBitmap(gw, gh, Bitmap.Config.ARGB_8888)
            map.setPixels(pixels, 0, gw, 0, 0, gw, gh)
            Api34.apply(bitmap, map, meta)
        }
    }

    private const val META_SIZE = 8

    private object Api34 {
        fun apply(bitmap: Bitmap, map: Bitmap, meta: FloatArray) {
            val gainmap = Gainmap(map)
            gainmap.setRatioMin(meta[2], meta[3], meta[4])
            gainmap.setRatioMax(meta[5], meta[6], meta[7])
            gainmap.setGamma(1f, 1f, 1f)
            gainmap.setEpsilonSdr(1f / 64f, 1f / 64f, 1f / 64f)
            gainmap.setEpsilonHdr(1f / 64f, 1f / 64f, 1f / 64f)
            gainmap.setMinDisplayRatioForHdrTransition(1f)
            gainmap.setDisplayRatioForFullHdr(maxOf(meta[5], meta[6], meta[7], 1.0001f))
            bitmap.setGainmap(gainmap)
        }
    }
}
