package com.frameout.app.trim

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class SourceProbe(val keyframesUs: LongArray, val fps: Float) {
    fun keyframesIn(startMs: Long, endMs: Long): Int {
        val from = lowerBound(startMs * 1000L)
        val to = lowerBound(endMs * 1000L)
        return (to - from).coerceAtLeast(0)
    }

    private fun lowerBound(us: Long): Int {
        var lo = 0
        var hi = keyframesUs.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (keyframesUs[mid] < us) lo = mid + 1 else hi = mid
        }
        return lo
    }

    companion object {
        private const val MAX_KEYFRAMES = 200_000
        private const val TIMEOUT_MS = 8_000L
        private val cache = object : LinkedHashMap<String, SourceProbe>(8, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SourceProbe>?) = size > 6
        }

        suspend fun load(context: Context, uri: Uri): SourceProbe? {
            val key = uri.toString()
            synchronized(cache) { cache[key] }?.let { return it }
            val probe = withTimeoutOrNull(TIMEOUT_MS) { withContext(Dispatchers.IO) { scan(context, uri) } }
            if (probe != null) synchronized(cache) { cache[key] = probe }
            return probe
        }

        private fun scan(context: Context, uri: Uri): SourceProbe? {
            val extractor = MediaExtractor()
            return try {
                extractor.setDataSource(context, uri, null)
                val track = (0 until extractor.trackCount).firstOrNull {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
                } ?: return null
                val format = extractor.getTrackFormat(track)
                val fps = if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) runCatching { format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() }.getOrDefault(0f) else 0f
                extractor.selectTrack(track)
                val times = ArrayList<Long>()
                extractor.seekTo(0L, MediaExtractor.SEEK_TO_NEXT_SYNC)
                var last = Long.MIN_VALUE
                while (times.size < MAX_KEYFRAMES) {
                    val t = extractor.sampleTime
                    if (t < 0 || t <= last) break
                    times += t
                    last = t
                    extractor.seekTo(t + 1, MediaExtractor.SEEK_TO_NEXT_SYNC)
                }
                if (times.isEmpty()) null else SourceProbe(times.toLongArray(), fps)
            } catch (e: Exception) {
                null
            } finally {
                runCatching { extractor.release() }
            }
        }
    }
}

@Composable
internal fun rememberSourceProbe(uri: Uri): SourceProbe? {
    val context = LocalContext.current.applicationContext
    val probe by produceState<SourceProbe?>(null, uri) { value = SourceProbe.load(context, uri) }
    return probe
}

@Composable
internal fun TrimEnv.frameEstimate(stored: String): Int {
    val probe = rememberSourceProbe(source)
    return estimateFrames(player.durationMs, clamped, stored, probe)
}
