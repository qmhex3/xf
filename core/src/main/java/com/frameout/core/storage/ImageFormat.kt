package com.frameout.core.storage

import android.graphics.Bitmap
import android.os.Build
import com.frameout.core.edit.GainmapOps
import com.frameout.core.hdr.HdrSignalRegistry
import java.io.OutputStream

enum class ImageFormat(val key: String, val ext: String, val mime: String, val lossless: Boolean, val hdr: Boolean = false, val modern: Boolean = false) {
    Jpg("jpg", "jpg", "image/jpeg", false),
    JpgHdr("jpg_hdr", "jpg", "image/jpeg", false, hdr = true),
    Png("png", "png", "image/png", true),
    PngHdr16("png_hdr16", "png", "image/png", true, hdr = true),
    WebpLossy("webp", "webp", "image/webp", false),
    Heic("heic", "heic", "image/heic", false, modern = true),
    Avif("avif", "avif", "image/avif", false, modern = true);

    fun effectiveQuality(quality: Int): Int = if (lossless || hdr) 100 else quality.coerceIn(1, 100)

    fun isAvailable(sdk: Int = Build.VERSION.SDK_INT): Boolean = when (this) {
        JpgHdr, PngHdr16 -> sdk >= 34
        Heic -> ModernCodecs.heic(sdk)
        Avif -> ModernCodecs.avif(sdk)
        else -> true
    }

    fun compress(bitmap: Bitmap, quality: Int, out: OutputStream, exif: ByteArray? = null): Boolean = when {
        this == PngHdr16 -> HdrSignalRegistry.peek(bitmap)?.let { Png16Writer.write(it, exif, out); HdrSignalRegistry.drop(bitmap); true } ?: false
        modern -> ModernEncoder.encode(this, bitmap, effectiveQuality(quality), exif, out)
        this == Png && exif != null -> {
            val buffer = ExposedBuffer(bitmap.byteCount.coerceAtMost(64 * 1024 * 1024))
            val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, buffer)
            if (ok) PngMetadata.splice(buffer.raw(), buffer.length(), exif, out)
            ok
        }
        (this == Jpg || this == JpgHdr) && needsFlatten(bitmap) -> flattened(bitmap)?.let { compress(it, quality, out, exif).also { _ -> it.recycle() } } ?: false
        this == Jpg && Jpeg444.supports(bitmap) -> GainmapOps.keeping(bitmap, hdr) { Jpeg444.encode(bitmap, effectiveQuality(quality), out) }
        else -> {
            val q = effectiveQuality(quality)
            val sdk = Build.VERSION.SDK_INT
            val effort = if (webpIsLossless(q, sdk)) WEBP_LOSSLESS_EFFORT else q
            GainmapOps.keeping(bitmap, hdr) { bitmap.compress(compressFormat(sdk, q), effort, out) }
        }
    }

    private fun needsFlatten(bitmap: Bitmap): Boolean =
        !bitmap.isRecycled && bitmap.hasAlpha() && bitmap.config != Bitmap.Config.HARDWARE && !GainmapOps.has(bitmap)

    private fun flattened(bitmap: Bitmap): Bitmap? {
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        out.setHasAlpha(false)
        out.eraseColor(-1)
        android.graphics.Canvas(out).drawBitmap(bitmap, 0f, 0f, null)
        return out
    }

    @Suppress("DEPRECATION")
    private fun compressFormat(sdk: Int, quality: Int = 100): Bitmap.CompressFormat = when (this) {
        Jpg, JpgHdr -> Bitmap.CompressFormat.JPEG
        Png -> Bitmap.CompressFormat.PNG
        WebpLossy -> when {
            sdk >= 30 -> if (webpIsLossless(quality, sdk)) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY
            else -> Bitmap.CompressFormat.WEBP
        }
        PngHdr16 -> error("Encoded by Png16Writer")
        Heic, Avif -> error("Encoded by ModernEncoder")
    }

    companion object {
        val keys: List<String> = entries.map { it.key }

        fun availableKeys(sdk: Int = Build.VERSION.SDK_INT): List<String> = entries.filter { it.isAvailable(sdk) }.map { it.key }

        private const val LEGACY_WEBP_LOSSLESS = "webp_lossless"
        private const val WEBP_LOSSLESS_EFFORT = 75

        internal fun webpIsLossless(quality: Int, sdk: Int): Boolean = quality >= 100 && sdk >= 29

        fun fromKey(key: String?): ImageFormat =
            if (key == LEGACY_WEBP_LOSSLESS) WebpLossy else entries.firstOrNull { it.key == key } ?: Jpg

        fun resolve(
            key: String?,
            hasGainmap: Boolean,
            sdk: Int = Build.VERSION.SDK_INT,
            hasSignal: Boolean = false,
            allowed: (ImageFormat) -> Boolean = com.frameout.core.pro.ProAccess::formatAllowed
        ): ImageFormat {
            val chosen = fromKey(key).takeIf(allowed) ?: Jpg
            return when {
                !chosen.isAvailable(sdk) -> Jpg
                chosen == JpgHdr && !hasGainmap -> Jpg
                chosen == PngHdr16 && hasSignal -> PngHdr16
                chosen == PngHdr16 -> if (hasGainmap) JpgHdr else Png
                else -> chosen
            }
        }

        fun working(
            key: String?,
            hasGainmap: Boolean,
            sdk: Int = Build.VERSION.SDK_INT,
            allowed: (ImageFormat) -> Boolean = com.frameout.core.pro.ProAccess::formatAllowed
        ): ImageFormat {
            val base = fromKey(key).takeUnless { it.modern } ?: Jpg
            return resolve(base.key, hasGainmap, sdk, allowed = allowed)
        }
    }
}

private class ExposedBuffer(initial: Int) : java.io.ByteArrayOutputStream(initial.coerceAtLeast(32)) {
    fun raw(): ByteArray = buf
    fun length(): Int = count
}
