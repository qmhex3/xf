package com.frameout.app.trim

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import com.frameout.app.FrameOutApp
import com.frameout.core.export.ExportBitmaps
import com.frameout.core.export.WebpAnimWriter
import com.frameout.core.export.WebpFrameEncoder
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
import java.io.File

internal class ClipSaver(private val context: Context) {
    private val app = context.applicationContext as FrameOutApp
    private val saver = FileSaver(context)

    @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
    suspend fun saveMp4(uri: Uri, range: TrimRange, durationMs: Long = 0L, onProgress: (Int) -> Unit = {}): SaveResult {
        val expected = SaveSpace.clipBytes(context, uri, range.spanMs(), durationMs)
        if (!SaveSpace.fits(context, treeUri(), expected)) return SaveResult.fail(FrameStrings.noSpace())
        val temp = File(context.cacheDir, "clip_${System.nanoTime()}.mp4")
        return try {
            val result = SaveSpace.watchFile(temp, expected, onProgress) { withContext(Dispatchers.IO) { ClipTranscoder(context).trim(uri.toString(), range.startMs, range.endMs, temp) } }
            if (result !is ReassembleResult.Success) return failure()
            if (!hasRoom(result.sizeBytes)) return SaveResult.fail(FrameStrings.noSpace())
            val name = "${FrameStrings.appName()}_${System.currentTimeMillis()}.mp4"
            val saved = saver.save(name, "video/mp4", "Movies/${FrameStrings.appName()}", treeUri()) { out -> temp.inputStream().use { it.copyTo(out) } }
            if (saved != null) SaveResult.ok(FrameStrings.savedOk()) else SaveResult.fail(FrameStrings.saveFailed())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure(e)
        } finally {
            temp.delete()
        }
    }

    suspend fun saveWebp(uri: Uri, range: TrimRange, durationMs: Long = 0L, onProgress: (Int) -> Unit = {}): SaveResult {
        val samples = ClipSampling.plan(range.startMs, range.endMs) ?: return SaveResult.fail(ClipStrings.tooShort())
        if (!SaveSpace.fits(context, treeUri(), samples.timesMs.size * SaveSpace.WEBP_FRAME_BYTES)) return SaveResult.fail(FrameStrings.noSpace())
        val temp = File(context.cacheDir, "clip_${System.nanoTime()}.webp")
        return try {
            if (!withContext(Dispatchers.IO) { renderWebp(uri, samples, temp, onProgress) }) return failure()
            if (!hasRoom(temp.length())) return SaveResult.fail(FrameStrings.noSpace())
            val name = "${FrameStrings.appName()}_${System.currentTimeMillis()}.webp"
            val saved = saver.save(name, "image/webp", app.container.prefs.saveFolder.value, treeUri()) { out -> temp.inputStream().use { it.copyTo(out) } }
            if (saved == null) SaveResult.fail(FrameStrings.saveFailed())
            else if (samples.capped) SaveResult.ok(ClipStrings.webpLimit((ClipSampling.MAX_SPAN_MS / 1000L).toInt())) else SaveResult.ok(FrameStrings.savedOk())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure(e)
        } finally {
            temp.delete()
        }
    }

    private suspend fun renderWebp(uri: Uri, samples: ClipSamples, file: File, onProgress: (Int) -> Unit): Boolean {
        val grabber = FrameGrabber(context, uri)
        return try {
            val first = grabber.grab(samples.timesMs.first()) ?: return false
            val size = ExportBitmaps.fitSize(first.width, first.height, ClipSampling.SIDE)
            WebpAnimWriter(file, size.first, size.second).use { writer ->
                writer.begin()
                if (!append(writer, first, size, samples.frameMs)) return@use false
                onProgress(SaveSpace.percentOf(1, samples.timesMs.size))
                for ((index, ms) in samples.timesMs.drop(1).withIndex()) {
                    val source = grabber.grab(ms) ?: return@use false
                    if (!append(writer, source, size, samples.frameMs)) return@use false
                    onProgress(SaveSpace.percentOf(index + 2, samples.timesMs.size))
                }
                if (writer.frameCount != samples.timesMs.size) return@use false
                writer.finish()
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        } finally {
            grabber.close()
        }
    }

    private fun append(writer: WebpAnimWriter, source: Bitmap, size: Pair<Int, Int>, frameMs: Int): Boolean {
        var frame: Bitmap? = null
        return try {
            frame = ExportBitmaps.letterbox(source, size.first, size.second)
            frame.setHasAlpha(false)
            val encoded = WebpFrameEncoder.encode(frame, ClipSampling.QUALITY)
            writer.addFrame(encoded, frameMs)
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

    private fun hasRoom(bytes: Long): Boolean = app.container.storage.hasDestinationRoom(treeUri(), bytes)
    private fun treeUri(): String? = app.container.prefs.saveFolderUri.value.ifBlank { null }
}
