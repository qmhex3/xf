package com.frameout.app.viewer

import com.frameout.core.util.logged
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import com.frameout.core.db.FrameEntity
import com.frameout.core.edit.BitmapEditRenderer
import com.frameout.core.edit.FrameEdit
import com.frameout.core.media.MotionPhotoExtractor
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FrameBitmapLoader(private val context: Context) {
    private val sourceLoader = FrameSourceBitmapLoader(context)

    suspend fun load(frame: FrameEntity, sourceUri: Uri?, edit: FrameEdit = FrameEdit(frame.id)): Bitmap? = withContext(Dispatchers.IO) {
        try {
            loadUnsafe(frame, sourceUri, edit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFailure("FrameBitmapLoader", e)
            null
        } catch (e: OutOfMemoryError) {
            logFailure("FrameBitmapLoader", e)
            null
        }
    }

    private suspend fun loadUnsafe(frame: FrameEntity, sourceUri: Uri?, edit: FrameEdit): Bitmap? = withContext(Dispatchers.IO) {
        frame.fullPath?.let { path ->
            decodeFile(path)?.let { return@withContext BitmapEditRenderer.render(it, edit) }
        }
        sourceUri?.let { source ->
            sourceLoader.decodeAtPts(frame.projectId, MotionPhotoExtractor.resolveVideoSource(context, source), frame.ptsUs)
        }?.let { BitmapEditRenderer.render(it, edit) }
    }

    private fun decodeFile(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 4096 && bounds.outHeight <= 4096) return BitmapFactory.decodeFile(path, argbOptions())
        return decodeLargeTiled(path, bounds.outWidth, bounds.outHeight)
    }

    private fun decodeLargeTiled(path: String, width: Int, height: Int): Bitmap? = runCatching {
        val decoder = BitmapRegionDecoder.newInstance(path, false)
        val sample = largeImageSample(width, height)
        val outW = (width / sample).coerceAtLeast(1)
        val outH = (height / sample).coerceAtLeast(1)
        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val tile = 2048
        var top = 0
        while (top < height) {
            var left = 0
            while (left < width) {
                val right = (left + tile).coerceAtMost(width)
                val bottom = (top + tile).coerceAtMost(height)
                val src = decoder.decodeRegion(Rect(left, top, right, bottom), argbOptions(sample))
                canvas.drawBitmap(src, left / sample.toFloat(), top / sample.toFloat(), null)
                src.recycle()
                left = right
            }
            top += tile
        }
        decoder.recycle()
        result
    }.logged("FrameBitmapLoader").getOrNull()

    private fun largeImageSample(width: Int, height: Int): Int {
        var sample = 1
        val maxMemory = Runtime.getRuntime().maxMemory()
        val maxPixels = (maxMemory / 4L / 4L).coerceIn(12_000_000L, 48_000_000L)
        while (width / sample > 6144 || height / sample > 6144 || width.toLong() * height.toLong() / (sample.toLong() * sample.toLong()) > maxPixels) sample *= 2
        return sample
    }

    private fun argbOptions(sample: Int = 1) = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
}
