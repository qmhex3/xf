package com.frameout.app.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.FrameOutApp
import com.frameout.core.media.LatestVideo
import com.frameout.core.media.LatestVideoFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val SUGGEST_RECENT_MS = 60L * 60L * 1000L
internal const val SUGGEST_MAX_AGE_MS = 3L * 24L * 60L * 60L * 1000L

internal fun shouldSuggest(
    addedMs: Long,
    nowMs: Long,
    previousSessionMs: Long,
    videoId: Long,
    handledId: Long,
    lastOpenedUri: String,
    videoUri: String,
    onboardingDone: Boolean
): Boolean {
    if (!onboardingDone) return false
    if (previousSessionMs <= 0L) return false
    if (videoId == handledId) return false
    if (videoUri == lastOpenedUri) return false
    val age = nowMs - addedMs
    if (age < 0L || age > SUGGEST_MAX_AGE_MS) return false
    return addedMs > previousSessionMs || age <= SUGGEST_RECENT_MS
}

internal class VideoSuggestionUi(val video: LatestVideo, val cover: Bitmap)

private object SessionMark {
    private var previous: Long? = null

    @Synchronized
    fun previousSession(nowMs: Long, prefs: com.frameout.core.prefs.AppPrefs): Long {
        previous?.let { return it }
        val value = prefs.lastLibrarySession()
        previous = value
        prefs.setLastLibrarySession(nowMs)
        return value
    }
}

internal class VideoSuggestionHolder(val ui: VideoSuggestionUi?, val handled: () -> Unit)

@Composable
internal fun rememberVideoSuggestion(): VideoSuggestionHolder {
    val context = LocalContext.current
    val app = context.applicationContext as FrameOutApp
    val prefs = app.container.prefs
    var current by remember { mutableStateOf<VideoSuggestionUi?>(null) }
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val previous = SessionMark.previousSession(now, prefs)
        current = withContext(Dispatchers.IO) {
            val sources = runCatching { app.container.projects.allSourceUris().toSet() }.getOrDefault(emptySet())
            val video = LatestVideoFinder.find(context, sources) ?: return@withContext null
            val ok = shouldSuggest(
                video.addedMs, now, previous, video.id, prefs.suggestionHandledId(),
                prefs.lastVideoUri(), video.uri.toString(), prefs.onboardingDone()
            )
            if (!ok) return@withContext null
            val cover = coverOf(context, video) ?: return@withContext null
            VideoSuggestionUi(video, cover)
        }
    }
    return VideoSuggestionHolder(current) {
        current?.let { prefs.setSuggestionHandledId(it.video.id) }
        current = null
    }
}

private fun coverOf(context: Context, video: LatestVideo): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= 29) {
        context.contentResolver.loadThumbnail(video.uri, Size(160, 160), null)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Video.Thumbnails.getThumbnail(context.contentResolver, video.id, MediaStore.Video.Thumbnails.MINI_KIND, BitmapFactory.Options())
    }
}.getOrNull() ?: runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, video.uri)
        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } finally {
        retriever.release()
    }
}.getOrNull()
