package com.frameout.core.storage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object SourceLocation {
    fun read(context: Context, uriString: String?): GeoPoint? {
        if (uriString.isNullOrBlank()) return null
        val uri = Uri.parse(uriString)
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
        return if (mime.startsWith("image/")) fromImage(context, uri) else fromVideo(context, uri) ?: fromImage(context, uri)
    }

    private fun original(context: Context, uri: Uri): Uri {
        if (Build.VERSION.SDK_INT < 29) return uri
        if (uri.authority != MediaStore.AUTHORITY) return uri
        if (context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) return uri
        return runCatching { MediaStore.setRequireOriginal(uri) }.getOrDefault(uri)
    }

    private fun fromImage(context: Context, uri: Uri): GeoPoint? = runCatching {
        context.contentResolver.openInputStream(original(context, uri))?.use { stream ->
            val exif = ExifInterface(stream)
            GeoPoint.fromExif(
                exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
                exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF),
                exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),
                exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
            )
        }
    }.getOrNull()

    private fun fromVideo(context: Context, uri: Uri): GeoPoint? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, original(context, uri))
            GeoPoint.parseIso6709(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION))
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
