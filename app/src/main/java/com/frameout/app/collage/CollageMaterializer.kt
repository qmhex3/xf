package com.frameout.app.collage

import com.frameout.core.util.logFailure
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.frameout.core.media.MotionPhotoExtractor
import com.frameout.engine.decode.FrameDecoder
import com.frameout.engine.decode.FramePuller
import com.frameout.core.db.FrameEditDao
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectDao
import com.frameout.core.edit.BitmapEditRenderer
import com.frameout.core.edit.FrameEdit
import com.frameout.core.edit.toEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CollageMaterializer(
    private val context: Context, private val projects: ProjectDao, private val edits: FrameEditDao
) {
    suspend fun materialize(frames: List<FrameEntity>): List<String?> = withContext(Dispatchers.IO) {
        frames.map { frame ->
            try {
                materializeFrame(frame)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                logFailure("CollageMaterializer", e)
                null
            }
        }
    }

    suspend fun rendered(frame: FrameEntity, maxSide: Int): Bitmap? = withContext(Dispatchers.IO) {
        val source = runCatching { sourceBitmap(frame, maxSide * 2) }.getOrNull() ?: return@withContext null
        val edit = edits.get(frame.id)?.toEdit() ?: FrameEdit(frame.id)
        val edited = runCatching { BitmapEditRenderer.render(source, edit) }.getOrNull()
        if (edited == null) { source.recycle(); return@withContext null }
        if (edited !== source) source.recycle()
        val longest = maxOf(edited.width, edited.height)
        if (longest <= maxSide) return@withContext edited
        val scale = maxSide.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(edited, (edited.width * scale).toInt().coerceAtLeast(1), (edited.height * scale).toInt().coerceAtLeast(1), true)
        if (scaled !== edited) edited.recycle()
        scaled
    }

    private suspend fun materializeFrame(frame: FrameEntity): String? {
        val source = sourceBitmap(frame) ?: return null
        val edit = edits.get(frame.id)?.toEdit() ?: FrameEdit(frame.id)
        val rendered = BitmapEditRenderer.render(source, edit)
        if (rendered !== source) source.recycle()
        val file = File(context.cacheDir, "frameout_collage/frame_${frame.id}.png")
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(temp).use { rendered.compress(Bitmap.CompressFormat.PNG, 100, it) }
        rendered.recycle()
        if (file.exists()) file.delete()
        if (!temp.renameTo(file)) { temp.delete(); return null }
        return file.absolutePath
    }

    private suspend fun sourceBitmap(frame: FrameEntity, limit: Int = 2048): Bitmap? {
        frame.fullPath?.let { path -> if (File(path).exists()) return decodeSized(path, limit) }
        val project = projects.getById(frame.projectId) ?: return null
        val source = MotionPhotoExtractor.resolveVideoSource(context, Uri.parse(project.sourceUri))
        return decodeExact(source, frame.ptsUs) ?: frame.thumbnailPath?.let(BitmapFactory::decodeFile)
    }
    private fun decodeExact(source: Uri, targetUs: Long): Bitmap? {
        val decoder = FrameDecoder(context)
        return try {
            decoder.open(source)
            decoder.seekToKeyframe(targetUs)
            FramePuller.pull(decoder, targetUs, 180)?.second
        } catch (e: Exception) {
            logFailure("CollageMaterializer", e)
            null
        } finally {
            decoder.close()
        }
    }

    private fun decodeSized(path: String, limit: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > limit || bounds.outHeight / sample > limit) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 })
    }

}
