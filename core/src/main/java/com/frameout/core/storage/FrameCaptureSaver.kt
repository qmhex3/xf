package com.frameout.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FrameCaptureSaver(private val context: Context, private val prefs: AppPrefs) {
    private val storage = StorageManager(context)
    private val exif = ExifWriter(context)

    suspend fun save(bitmap: Bitmap, format: ImageFormat, source: CaptureSource): CaptureSave = withContext(Dispatchers.IO) {
        val tree = prefs.saveFolderUri.value.ifBlank { null }
        if (!storage.hasDestinationRoom(tree, bitmap.byteCount.toLong())) return@withContext CaptureSave.Failed(CaptureSave.Reason.NoSpace)
        val project = SourceInfo.name(context, source.uri)
        val stamp = stampFor(project, source)
        val name = CaptureNaming.name(prefs.namingTemplate.value, project, source.positionMs, source.sequence, format.ext)
        val block = FormatMetadata.block(format, stamp)
        val uri = storage.saveBitmapToGallery(bitmap, name, format, prefs.jpegQuality.value, prefs.saveFolder.value, stamp.takenAt, tree, "", block)
            ?: return@withContext CaptureSave.Failed(CaptureSave.Reason.Unwritable)
        val metadataExpected = format.modern || block != null || exif.supports(format, stamp)
        val metadataWritten = when {
            format == ImageFormat.Png || format == ImageFormat.PngHdr16 -> block != null
            format.modern -> block != null
            exif.supports(format, stamp) -> exif.write(uri, format, stamp)
            else -> true
        }
        if (hasContent(uri)) CaptureSave.Saved(uri, format.mime, name, !metadataExpected || metadataWritten) else discard(uri)
    }

    fun failureMessage(failed: CaptureSave.Failed): String = when (failed.reason) {
        CaptureSave.Reason.NoSpace -> FrameStrings.noSpace()
        CaptureSave.Reason.Empty -> FrameStrings.saveEmpty()
        CaptureSave.Reason.Unwritable -> SaveFailure.message(context, FrameStrings.saveFailed())
    }

    private fun stampFor(project: String, source: CaptureSource): ExifStamp {
        val base = SourceDate.read(context, source.uri) ?: System.currentTimeMillis()
        val takenAt = if (prefs.datePolicy.value == "video") base else base + source.positionMs
        val keep = prefs.keepLocation.value
        val location = if (keep) SourceLocation.read(context, source.uri) else null
        return ExifStamp(takenAt, ExifText.description(project, source.sequence, source.positionMs * 1000L), location, keep)
    }

    private suspend fun discard(uri: Uri): CaptureSave {
        storage.deleteGalleryUri(uri.toString())
        return CaptureSave.Failed(CaptureSave.Reason.Empty)
    }

    private fun hasContent(uri: Uri): Boolean = runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length != 0L } ?: false
    }.getOrDefault(false)
}
