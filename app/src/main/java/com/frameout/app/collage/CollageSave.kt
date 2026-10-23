package com.frameout.app.collage

import com.frameout.core.storage.insertTracked
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.toArgb
import com.frameout.app.FrameOutApp
import com.frameout.app.trim.SavedCapture
import com.frameout.core.collage.COLLAGE_CORNER_REFERENCE
import com.frameout.core.collage.CollageConfig
import com.frameout.core.collage.CollageRenderer
import com.frameout.core.db.SavedImageEntity
import com.frameout.core.storage.ImageFormat
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ThumbCache
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val PREVIEW_MAX_SIDE = 900

suspend fun loadPreview(path: String?): Bitmap? {
    if (path.isNullOrBlank()) return null
    val file = File(path)
    if (!file.exists()) return null
    return ThumbCache.loadKeyed("collage_prev:$path:${file.lastModified()}:$PREVIEW_MAX_SIDE") {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > PREVIEW_MAX_SIDE || bounds.outHeight / sample > PREVIEW_MAX_SIDE) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}

private fun CollageState.config() = CollageConfig(
    template = canvasTemplate,
    spacingPx = gap,
    backgroundColor = background.toArgb().toLong() and 0xFFFFFFFFL,
    outputWidth = resolution,
    cornerRadiusPx = if (rounded) COLLAGE_CORNER_REFERENCE else 0f,
    timestamps = if (stamps) stampsInOrder() else emptyList(),
    crops = cropsInOrder()
)

suspend fun CollageState.save(app: FrameOutApp): SavedCapture? {
    var bitmap: Bitmap? = null
    return try {
        val ordered = order.map { paths.getOrNull(it) }
        val rendered = withContext(Dispatchers.Default) { CollageRenderer.render(ordered, config()) }
        bitmap = rendered
        store(app, rendered).also { com.frameout.core.rating.Rating.success() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logFailure("CollageSave", e)
        com.frameout.core.rating.Rating.failure()
        null
    } catch (e: OutOfMemoryError) {
        null
    } finally {
        bitmap?.recycle()
    }
}

private suspend fun CollageState.store(app: FrameOutApp, bitmap: Bitmap): SavedCapture? {
    val prefs = app.container.prefs
    val format = ImageFormat.resolve(prefs.saveFormat.value, false)
    val name = "FrameOut_collage_${System.currentTimeMillis()}.${format.ext}"
    val first = frames.getOrNull(order.firstOrNull() ?: 0)
    val subfolder = app.container.projects.getById(first?.projectId ?: -1L)?.saveSubfolder.orEmpty()
    val uri = app.container.storage.saveBitmapToGallery(
        bitmap, name, format, prefs.jpegQuality.value, prefs.saveFolder.value, null, prefs.saveFolderUri.value.ifBlank { null }, subfolder
    ) ?: return null
    val entity = SavedImageEntity(
        uri = uri.toString(), displayName = name, album = FrameStrings.appName(),
        width = bitmap.width, height = bitmap.height, createdAt = System.currentTimeMillis()
    )
    app.container.savedImages.insertTracked(app.container.storage, entity)
    return SavedCapture(uri, format.mime)
}
