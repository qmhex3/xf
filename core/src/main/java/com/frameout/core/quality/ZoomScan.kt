package com.frameout.core.quality

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class ZoomCrop(val bitmap: Bitmap, val scale: Float, private val owned: Boolean = true) {
    fun release() {
        if (owned && !bitmap.isRecycled) bitmap.recycle()
    }
}

internal class ZoomTile(val rect: PxRect, val crop: ZoomCrop) {
    val bitmap: Bitmap get() = crop.bitmap

    fun toFrame(left: Int, top: Int, right: Int, bottom: Int): PxRect {
        val s = crop.scale
        val x = rect.x + (left / s).roundToInt()
        val y = rect.y + (top / s).roundToInt()
        val r = rect.x + (right / s).roundToInt()
        val b = rect.y + (bottom / s).roundToInt()
        return PxRect(x, y, (r - x).coerceAtLeast(1), (b - y).coerceAtLeast(1))
    }
}

internal object ZoomScan {
    fun crop(source: Bitmap, rect: PxRect, target: Int, maxScale: Float): ZoomCrop? {
        if (source.isRecycled) return null
        val x = rect.x.coerceIn(0, source.width - 1)
        val y = rect.y.coerceIn(0, source.height - 1)
        val w = rect.w.coerceAtMost(source.width - x)
        val h = rect.h.coerceAtMost(source.height - y)
        if (w < 8 || h < 8) return null
        val piece = Bitmap.createBitmap(source, x, y, w, h)
        val own = piece !== source
        return enlarge(piece, target, maxScale, own)
    }

    fun enlarge(piece: Bitmap, target: Int, maxScale: Float, owned: Boolean = true): ZoomCrop {
        val scale = SmartZoom.scaleFor(piece.width, piece.height, target, maxScale)
        if (scale <= 1.01f) return ZoomCrop(piece, 1f, owned)
        val up = Bitmap.createScaledBitmap(piece, (piece.width * scale).roundToInt(), (piece.height * scale).roundToInt(), true)
        if (up === piece) return ZoomCrop(piece, 1f, owned)
        if (owned) piece.recycle()
        return ZoomCrop(up, scale)
    }

    inline fun forEachTile(source: Bitmap, block: (ZoomTile) -> Unit) {
        for (rect in SmartZoom.grid(source.width, source.height)) {
            val crop = crop(source, rect, SmartZoom.TILE_TARGET, SmartZoom.MAX_UPSCALE) ?: continue
            try {
                block(ZoomTile(rect, crop))
            } finally {
                crop.release()
            }
        }
    }
}

internal class FullResRegions private constructor(private val path: String, private var decoder: BitmapRegionDecoder?) : AutoCloseable {
    private var fallback: Bitmap? = null
    private var fallbackTried = false

    fun crop(rect: PxRect, frameWidth: Int, frameHeight: Int, target: Int, maxScale: Float): ZoomCrop? {
        if (frameWidth <= 0 || frameHeight <= 0) return null
        fromDecoder(rect, frameWidth, frameHeight, target, maxScale)?.let { return it }
        return fromFallback(rect, frameWidth, frameHeight, target, maxScale)
    }

    private fun fromDecoder(rect: PxRect, frameWidth: Int, frameHeight: Int, target: Int, maxScale: Float): ZoomCrop? {
        val d = decoder ?: return null
        return try {
            val dw = d.width
            val dh = d.height
            val region = regionOf(rect, frameWidth, frameHeight, dw, dh)
            if (region.width() < 8 || region.height() < 8) return null
            var sample = 1
            while (min(region.width(), region.height()) / (sample * 2) >= target) sample *= 2
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val raw = d.decodeRegion(region, options)
            if (raw == null) {
                decoder = null
                null
            } else {
                ZoomScan.enlarge(raw, target, maxScale)
            }
        } catch (e: Exception) {
            decoder = null
            null
        }
    }

    private fun fromFallback(rect: PxRect, frameWidth: Int, frameHeight: Int, target: Int, maxScale: Float): ZoomCrop? {
        val big = fallbackBitmap() ?: return null
        val region = regionOf(rect, frameWidth, frameHeight, big.width, big.height)
        if (region.width() < 8 || region.height() < 8) return null
        return try {
            val piece = Bitmap.createBitmap(big, region.left, region.top, region.width(), region.height())
            ZoomScan.enlarge(piece, target, maxScale, piece !== big)
        } catch (e: Exception) {
            null
        }
    }

    private fun fallbackBitmap(): Bitmap? {
        if (fallbackTried) return fallback
        fallbackTried = true
        fallback = try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val longSide = max(bounds.outWidth, bounds.outHeight)
            if (longSide <= 0) {
                null
            } else {
                var sample = 1
                while (longSide / (sample * 2) >= FALLBACK_SIDE) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                })
            }
        } catch (e: Throwable) {
            null
        }
        return fallback
    }

    private fun regionOf(rect: PxRect, frameWidth: Int, frameHeight: Int, dw: Int, dh: Int): Rect {
        val left = (rect.x.toFloat() / frameWidth * dw).roundToInt().coerceIn(0, dw - 1)
        val top = (rect.y.toFloat() / frameHeight * dh).roundToInt().coerceIn(0, dh - 1)
        val right = (rect.r.toFloat() / frameWidth * dw).roundToInt().coerceIn(left + 1, dw)
        val bottom = (rect.b.toFloat() / frameHeight * dh).roundToInt().coerceIn(top + 1, dh)
        return Rect(left, top, right, bottom)
    }

    override fun close() {
        try {
            decoder?.recycle()
        } catch (e: Exception) {
        }
        decoder = null
        fallback?.let { if (!it.isRecycled) it.recycle() }
        fallback = null
    }

    companion object {
        const val FALLBACK_SIDE = 2048

        @Suppress("DEPRECATION")
        fun open(path: String?): FullResRegions? {
            if (path.isNullOrBlank()) return null
            val decoder = try {
                BitmapRegionDecoder.newInstance(path, false)
            } catch (e: Exception) {
                null
            }
            return FullResRegions(path, decoder)
        }
    }
}
