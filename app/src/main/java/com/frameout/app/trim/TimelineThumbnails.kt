package com.frameout.app.trim

import com.frameout.core.util.SmoothScale
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FormatProbe
import com.frameout.engine.decode.AnimatedFrameSource
import com.frameout.engine.decode.ApngFrameDecoder
import com.frameout.engine.decode.DecodeRouter
import com.frameout.engine.decode.GifFrameDecoder
import com.frameout.engine.decode.ImageHead
import com.frameout.engine.decode.WebpFrameDecoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

internal const val THUMB_SLOTS = 10

internal fun thumbSlotsFor(durationMs: Long): Int = when {
    durationMs <= 120_000L -> THUMB_SLOTS
    durationMs <= 1_800_000L -> 14
    else -> 18
}
private const val THUMB_MAX = 320

private object TimelineThumbCache {
    private val entries = android.util.LruCache<String, Array<Bitmap?>>(4)
    fun get(key: String): Array<Bitmap?>? = entries.get(key)
    fun put(key: String, value: Array<Bitmap?>) {
        entries.put(key, value)
    }
}

private object TimelineThumbDisk {
    private const val DIR = "timeline_thumbs"
    private const val MAX_BYTES = 24L * 1024L * 1024L

    private fun dir(context: Context) = java.io.File(context.cacheDir, DIR).also { it.mkdirs() }

    private fun name(key: String, slot: Int): String {
        val hash = java.security.MessageDigest.getInstance("SHA-1").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return "${hash}_$slot.jpg"
    }

    fun get(context: Context, key: String, slots: Int): Array<Bitmap?>? {
        val folder = dir(context)
        val out = arrayOfNulls<Bitmap>(slots)
        for (slot in 0 until slots) {
            val file = java.io.File(folder, name(key, slot))
            val bitmap = if (file.isFile) runCatching { android.graphics.BitmapFactory.decodeFile(file.absolutePath) }.getOrNull() else null
            if (bitmap == null) return null
            file.setLastModified(System.currentTimeMillis())
            out[slot] = bitmap
        }
        return out
    }

    fun put(context: Context, key: String, thumbs: Array<Bitmap?>) {
        if (thumbs.any { it == null }) return
        runCatching {
            val folder = dir(context)
            thumbs.forEachIndexed { slot, bitmap ->
                val temp = java.io.File(folder, name(key, slot) + ".tmp")
                temp.outputStream().use { bitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                if (!temp.renameTo(java.io.File(folder, name(key, slot)))) temp.delete()
            }
            trim(folder)
        }
    }

    private fun trim(folder: java.io.File) {
        val files = folder.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= MAX_BYTES) break
            total -= file.length()
            file.delete()
        }
    }
}

@Composable
internal fun rememberTimelineThumbs(context: Context, source: Uri?, durationMs: Long): List<Bitmap?> {
    val slots = thumbSlotsFor(durationMs)
    val thumbs = remember(source, durationMs) {
        mutableStateListOf<Bitmap?>().apply { repeat(slots) { add(null) } }
    }
    LaunchedEffect(source, durationMs) {
        if (source == null || durationMs <= 0L) return@LaunchedEffect
        val key = "$source|$durationMs"
        val cached = TimelineThumbCache.get(key)
            ?: kotlinx.coroutines.withContext(Dispatchers.IO) { TimelineThumbDisk.get(context.applicationContext, key, slots) }
                ?.also { TimelineThumbCache.put(key, it) }
        if (cached != null) {
            cached.forEachIndexed { slot, bitmap -> if (slot < slots) thumbs[slot] = bitmap }
            return@LaunchedEffect
        }
        val channel = Channel<Pair<Int, Bitmap>>(Channel.UNLIMITED)
        val job = launch(Dispatchers.IO) {
            try {
                loadTimelineThumbs(context.applicationContext, source, durationMs, slots) { slot, bitmap -> channel.trySend(slot to bitmap) }
            } finally {
                channel.close()
            }
        }
        val collected = arrayOfNulls<Bitmap>(slots)
        try {
            for ((slot, bitmap) in channel) {
                thumbs[slot] = bitmap
                collected[slot] = bitmap
            }
            if (collected.any { it != null }) TimelineThumbCache.put(key, collected)
            if (collected.all { it != null }) {
                val app = context.applicationContext
                kotlinx.coroutines.withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) { TimelineThumbDisk.put(app, key, collected) }
            }
        } finally {
            job.cancel()
        }
    }
    return thumbs
}

private suspend fun loadTimelineThumbs(context: Context, uri: Uri, durationMs: Long, slots: Int, emit: (Int, Bitmap) -> Unit) {
    if (ImageHead.isAnimatedWebp(context, uri)) return loadAnimatedThumbs(WebpFrameDecoder(context), uri, slots, emit)
    when (val kind = FormatProbe.probe(context, uri).kind) {
        FormatKind.Gif, FormatKind.Apng -> loadAnimatedThumbs(context, uri, kind, slots, emit)
        FormatKind.NeedsFfmpeg -> loadFfmpegThumbs(context, uri, slots, emit)
        else -> loadRetrieverThumbs(context, uri, durationMs, slots, emit)
    }
}

private class FrameGrabber(private val retriever: MediaMetadataRetriever) {
    private val width: Int
    private val height: Int
    private val ratio: Float
    private var scaled = Build.VERSION.SDK_INT >= 27

    init {
        val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        val displayW = if (rotation == 90 || rotation == 270) h else w
        val displayH = if (rotation == 90 || rotation == 270) w else h
        if (displayW > 0 && displayH > 0) {
            val factor = THUMB_MAX.toFloat() / maxOf(displayW, displayH)
            width = maxOf(2, (displayW * factor).toInt())
            height = maxOf(2, (displayH * factor).toInt())
            ratio = displayW.toFloat() / displayH
        } else {
            width = THUMB_MAX
            height = THUMB_MAX * 9 / 16
            ratio = 0f
        }
    }

    fun frameAt(timeUs: Long): Bitmap? {
        if (scaled && Build.VERSION.SDK_INT >= 27) {
            val bitmap = retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, width, height)
            if (bitmap != null) {
                if (ratio <= 0f || kotlin.math.abs(bitmap.width.toFloat() / bitmap.height / ratio - 1f) < 0.1f) return bitmap
                bitmap.recycle()
                scaled = false
            }
        }
        return retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let(::scaleThumb)
    }
}

private suspend fun loadRetrieverThumbs(context: Context, uri: Uri, durationMs: Long, slots: Int, emit: (Int, Bitmap) -> Unit) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        val grabber = FrameGrabber(retriever)
        for (slot in 0 until slots) {
            currentCoroutineContext().ensureActive()
            val timeUs = (slot * 2L + 1L) * durationMs * 1000L / (slots * 2L)
            val bitmap = runCatching { grabber.frameAt(timeUs) }.getOrNull()
            if (bitmap != null) emit(slot, bitmap)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
    } finally {
        runCatching { retriever.release() }
    }
}

private fun slotIndex(slot: Int, count: Int, slots: Int): Int =
    ((slot * 2L + 1L) * count / (slots * 2L)).toInt().coerceIn(0, count - 1)

private suspend fun loadAnimatedThumbs(context: Context, uri: Uri, kind: FormatKind, slots: Int, emit: (Int, Bitmap) -> Unit) =
    loadAnimatedThumbs(if (kind == FormatKind.Gif) GifFrameDecoder(context) else ApngFrameDecoder(context), uri, slots, emit)

private suspend fun loadAnimatedThumbs(decoder: AnimatedFrameSource, uri: Uri, slots: Int, emit: (Int, Bitmap) -> Unit) {
    try {
        decoder.open(uri)
        val count = decoder.frameCount()
        if (count <= 0) return
        for (slot in 0 until slots) {
            currentCoroutineContext().ensureActive()
            decoder.decodeFrame(slotIndex(slot, count, slots))?.second?.let { emit(slot, scaleThumb(it)) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
    } finally {
        runCatching { decoder.close() }
    }
}

private suspend fun loadFfmpegThumbs(context: Context, uri: Uri, slots: Int, emit: (Int, Bitmap) -> Unit) {
    runCatching {
        val database = com.frameout.core.db.AppDatabase.get(context)
        val projectId = database.projectDao().getBySourceUri(uri.toString())?.id ?: return@runCatching
        val dao = database.videoIndexDao()
        val count = dao.count(projectId)
        if (count == 0) return@runCatching
        val router = DecodeRouter(context)
        try {
            router.open(uri)
            for (slot in 0 until slots) {
                currentCoroutineContext().ensureActive()
                val entity = dao.page(projectId, 1, slotIndex(slot, count, slots)).firstOrNull() ?: continue
                if (entity.nearestKeyframeIndex >= 0) router.seekFfmpegToKeyframe(entity.nearestKeyframeIndex, entity.nearestKeyframePtsUs)
                router.decodeFfmpegFrame(entity.sampleIndex)?.bitmap?.let { emit(slot, scaleThumb(it)) }
            }
        } finally {
            router.close()
        }
    }
}

private fun scaleThumb(bitmap: Bitmap): Bitmap {
    if (bitmap.width <= THUMB_MAX && bitmap.height <= THUMB_MAX) return bitmap
    val factor = THUMB_MAX.toFloat() / maxOf(bitmap.width, bitmap.height)
    val scaled = SmoothScale.scaled(bitmap, (bitmap.width * factor).toInt(), (bitmap.height * factor).toInt(), true)
    if (scaled !== bitmap) bitmap.recycle()
    return scaled
}
