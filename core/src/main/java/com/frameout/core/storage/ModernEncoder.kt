package com.frameout.core.storage

import com.frameout.core.util.logFailure
import android.graphics.Bitmap
import android.os.Build
import androidx.heifwriter.AvifWriter
import androidx.heifwriter.HeifWriter
import java.io.File
import java.io.OutputStream

internal object ModernEncoder {
    private const val STOP_TIMEOUT_MS = 30_000L

    fun encode(format: ImageFormat, bitmap: Bitmap, quality: Int, exif: ByteArray?, out: OutputStream): Boolean {
        if (Build.VERSION.SDK_INT < ModernCodecs.MIN_SDK || bitmap.isRecycled) return false
        val source = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return false
        val temp = File.createTempFile("fx_modern", ".${format.ext}")
        return try {
            if (format == ImageFormat.Avif) writeAvif(temp.absolutePath, source, quality, exif) else writeHeic(temp.absolutePath, source, quality, exif)
            temp.inputStream().use { it.copyTo(out) }
            true
        } catch (e: Exception) {
            logFailure("ModernEncoder", e)
            false
        } finally {
            temp.delete()
            if (source !== bitmap) source.recycle()
        }
    }

    private fun writeHeic(path: String, bitmap: Bitmap, quality: Int, exif: ByteArray?) {
        HeifWriter.Builder(path, bitmap.width, bitmap.height, HeifWriter.INPUT_MODE_BITMAP).setQuality(quality).setMaxImages(1).setPrimaryIndex(0).build().use { writer ->
            writer.start()
            writer.addBitmap(bitmap)
            if (exif != null) writer.addExifData(0, exif, 0, exif.size)
            writer.stop(STOP_TIMEOUT_MS)
        }
    }

    private fun writeAvif(path: String, bitmap: Bitmap, quality: Int, exif: ByteArray?) {
        AvifWriter.Builder(path, bitmap.width, bitmap.height, AvifWriter.INPUT_MODE_BITMAP).setQuality(quality).setMaxImages(1).setPrimaryIndex(0).build().use { writer ->
            writer.start()
            writer.addBitmap(bitmap)
            if (exif != null) writer.addExifData(0, exif, 0, exif.size)
            writer.stop(STOP_TIMEOUT_MS)
        }
    }
}
