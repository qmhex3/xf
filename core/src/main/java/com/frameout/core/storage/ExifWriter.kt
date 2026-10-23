package com.frameout.core.storage

import com.frameout.core.util.logged
import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.File

data class ExifStamp(val takenAt: Long, val description: String, val location: GeoPoint?, val pngMetadata: Boolean)

class ExifWriter(private val context: Context) {
    fun supports(format: ImageFormat, stamp: ExifStamp): Boolean = when (format) {
        ImageFormat.Jpg, ImageFormat.JpgHdr -> true
        ImageFormat.WebpLossy -> Build.VERSION.SDK_INT >= 31
        ImageFormat.Png, ImageFormat.PngHdr16 -> stamp.pngMetadata
        ImageFormat.Heic, ImageFormat.Avif -> false
    }

    fun embeddedAtEncode(format: ImageFormat): Boolean = format == ImageFormat.Png || format == ImageFormat.PngHdr16 || format.modern

    fun write(uri: Uri, format: ImageFormat, stamp: ExifStamp): Boolean {
        if (!supports(format, stamp)) return false
        return runCatching {
            if (format == ImageFormat.Png || format == ImageFormat.PngHdr16) true else writeTags(uri, stamp)
        }.logged("ExifWriter").getOrDefault(false)
    }

    fun writeFile(file: File, format: ImageFormat, stamp: ExifStamp): Boolean {
        if (!supports(format, stamp)) return false
        return runCatching {
            if (format == ImageFormat.Png || format == ImageFormat.PngHdr16) {
                true
            } else {
                applyExifTags(ExifInterface(file.absolutePath), stamp)
                true
            }
        }.logged("ExifWriter").getOrDefault(false)
    }

    private fun writeTags(uri: Uri, stamp: ExifStamp): Boolean =
        context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
            applyExifTags(ExifInterface(pfd.fileDescriptor), stamp)
            true
        } ?: false
}
