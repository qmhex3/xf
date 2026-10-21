package com.frameout.app.trim

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import com.frameout.app.FrameOutApp
import com.frameout.core.export.ExportBitmaps
import com.frameout.core.export.GifEncoder
import com.frameout.core.export.MotionPhotoWriter
import com.frameout.core.storage.FileSaver
import com.frameout.core.util.ClipStrings
import com.frameout.core.util.FrameStrings
import com.frameout.engine.edit.ClipTranscoder
import com.frameout.engine.edit.ReassembleResult
import com.frameout.engine.player.FrameGrabber
import com.frameout.engine.timeline.TrimRange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

internal class MotionClipSaver(private val context: Context) {
    private val app = context.applicationContext as FrameOutApp
    private val saver = FileSaver(context)

    suspend fun saveGif(uri: Uri, range: TrimRange, onProgress: (Int) -> Unit = {}): SaveResult {
        val samples = ClipSampling.plan(range.startMs, range.endMs, TrimOutput.Gif.maxMs, GIF_FPS, GIF_MAX_FRAMES) ?: return SaveResult.fail(ClipStrings.tooShort())
        if (!SaveSpace.fits(context, treeUri(), samples.timesMs.size * SaveSpace.GIF_FRAME_BYTES)) return SaveResult.fail(FrameStrings.noSpace())
        val temp = File(context.cacheDir, "clip_${System.nanoTime()}.gif")
        return try {
            if (!withContext(Dispatchers.IO) { renderGif(uri, samples, temp, onProgress) }) return failure()
            if (!hasRoom(temp.length())) return SaveResult.fail(FrameStrings.noSpace())
            val saved = saver.save(fileName("gif"), "image/gif", app.container.prefs.saveFolder.value, treeUri()) { out -> temp.inputStream().use { it.copyTo(out) } }
            if (saved != null) SaveResult.ok(FrameStrings.savedOk()) else SaveResult.fail(FrameStrings.saveFailed())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure(e)
        } finally {
            temp.delete()
        }
    }

    @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
    suspend fun saveLive(uri: Uri, range: TrimRange, positionMs: Long, durationMs: Long = 0L, onProgress: (Int) -> Unit = {}): SaveResult {
        val clip = TrimLimits.fit(range, TrimOutput.Live.maxMs)
        if (clip.spanMs() < MIN_LIVE_MS) return SaveResult.fail(ClipStrings.tooShort())
        val expected = SaveSpace.clipBytes(context, uri, clip.spanMs(), durationMs)
        if (!SaveSpace.fits(context, treeUri(), expected + SaveSpace.STILL_BYTES)) return SaveResult.fail(FrameStrings.noSpace())
        val video = File(context.cacheDir, "live_${System.nanoTime()}.mp4")
        return try {
            val result = SaveSpace.watchFile(video, expected, onProgress) { withContext(Dispatchers.IO) { ClipTranscoder(context).trim(uri.toString(), clip.startMs, clip.endMs, video) } }
            if (result !is ReassembleResult.Success) return failure()
            val stillAt = positionMs.coerceIn(clip.startMs, clip.endMs)
            val still = stillJpeg(uri, stillAt) ?: return failure()
            if (!hasRoom(video.length() + still.size)) return SaveResult.fail(FrameStrings.noSpace())
            val saved = saver.save(fileName("jpg"), "image/jpeg", app.container.prefs.saveFolder.value, treeUri()) { out ->
                MotionPhotoWriter.write(still, video, (stillAt - clip.startMs) * 1000L, out)
            }
            if (saved != null) SaveResult.ok(FrameStrings.savedOk()) else SaveResult.fail(FrameStrings.saveFailed())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure(e)
        } finally {
            video.delete()
        }
    }

    private suspend fun stillJpeg(uri: Uri, positionMs: Long): ByteArray? = withContext(Dispatchers.IO) {
        val grabber = FrameGrabber(context, uri)
        try {
            val bitmap = grabber.grab(positionMs) ?: return@withContext null
            try {
                ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, STILL_QUALITY, it) }.toByteArray()
            } finally {
                bitmap.recycle()
            }
        } finally {
            grabber.close()
        }
    }

    private suspend fun renderGif(uri: Uri, samples: ClipSamples, file: File, onProgress: (Int) -> Unit): Boolean {
        val grabber = FrameGrabber(context, uri)
        return try {
            val first = grabber.grab(samples.timesMs.first()) ?: return false
            val size = ExportBitmaps.fitSize(first.width, first.height, GIF_SIDE)
            file.outputStream().buffered().use { out ->
                val encoder = GifEncoder(out, size.first, size.second, samples.frameMs)
                encoder.begin()
                var ok = append(encoder, first, size)
                onProgress(SaveSpace.percentOf(1, samples.timesMs.size))
                for ((index, ms) in samples.timesMs.drop(1).withIndex()) {
                    if (!ok) break
                    val source = grabber.grab(ms)
                    ok = source != null && append(encoder, source, size)
                    onProgress(SaveSpace.percentOf(index + 2, samples.timesMs.size))
                }
                if (ok) encoder.finish()
                ok
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        } finally {
            grabber.close()
        }
    }

    private fun append(encoder: GifEncoder, source: Bitmap, size: Pair<Int, Int>): Boolean {
        var frame: Bitmap? = null
        return try {
            frame = ExportBitmaps.letterbox(source, size.first, size.second)
            encoder.addFrame(frame)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        } finally {
            frame?.takeUnless { it.isRecycled }?.recycle()
            if (!source.isRecycled) source.recycle()
        }
    }

    private fun failure(error: Throwable? = null): SaveResult =
        SaveResult.fail(if (SaveSpace.cacheLow(context) || (error != null && SaveSpace.isNoSpace(error))) FrameStrings.noSpace() else FrameStrings.saveFailed())

    private fun fileName(extension: String) = "${FrameStrings.appName()}_${System.currentTimeMillis()}.$extension"
    private fun hasRoom(bytes: Long): Boolean = app.container.storage.hasDestinationRoom(treeUri(), bytes)
    private fun treeUri(): String? = app.container.prefs.saveFolderUri.value.ifBlank { null }

    private companion object {
        const val GIF_SIDE = 360
        const val GIF_FPS = 10
        const val GIF_MAX_FRAMES = 80
        const val STILL_QUALITY = 95
        const val MIN_LIVE_MS = 500L
    }
}
