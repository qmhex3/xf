package com.frameout.core.media

import com.frameout.core.util.FrameStrings
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MediaScanner(private val context: Context) {
    private class Cached(val summary: MediaSummary, val version: Int, val access: Int, val at: Long)

    private companion object {
        const val SUMMARY_TTL_MS = 300_000L

        val summaryMutex = Mutex()
        @Volatile var cached: Cached? = null
    }

    private fun accessSignature(): Int =
        (if (PermissionHelper.canReadVideos(context)) 1 else 0) or
            (if (PermissionHelper.canReadImages(context)) 2 else 0) or
            (if (PermissionHelper.isPartialMediaAccess(context)) 4 else 0)

    fun pagingSource(bucketId: String?, kind: MediaKind?) = MediaStorePagingSource(context, bucketId, kind)

    suspend fun summary(force: Boolean = false): MediaSummary = summaryMutex.withLock {
        MediaChangeTracker.start(context)
        val now = System.currentTimeMillis()
        val version = MediaChangeTracker.version
        val access = accessSignature()
        cached?.takeIf { !force && it.version == version && it.access == access && now - it.at < SUMMARY_TTL_MS }?.let { return@withLock it.summary }
        val folder = FolderMediaIndex.loadSafely(context)
        val result = withContext(Dispatchers.IO) {
            buildSummary(folder)
        }
        cached = Cached(result, version, access, now)
        result
    }

    private fun buildSummary(folder: List<FolderEntry>): MediaSummary {
        val albums = linkedMapOf<String, Pair<String, Int>>()
        var all = 0
        var video = 0
        var gif = 0
        var live = 0
        var motion = 0
        var animated = 0
        scanSummary(folder) { item ->
            val key = item.bucketId.ifEmpty { "0" }
            val old = albums[key]
            albums[key] = (old?.first ?: item.bucketName.ifEmpty { "Media" }) to ((old?.second ?: 0) + 1)
            all++
            when (item.kind) {
                MediaKind.Video -> video++
                MediaKind.Gif -> gif++
                MediaKind.LivePhoto -> live++
                MediaKind.MotionPhoto -> motion++
                MediaKind.Animated -> animated++
                MediaKind.Image -> Unit
            }
        }
        val albumList = albums.map { (id, value) -> MediaAlbum(id, value.first, value.second) }.sortedByDescending { it.count }
        val filters = buildList {
            if (all > 0) add(MediaFilterCount(null, FrameStrings.allMedia(), all))
            if (video > 0) add(MediaFilterCount(MediaKind.Video, FrameStrings.mediaVideo(), video))
            if (gif > 0) add(MediaFilterCount(MediaKind.Gif, "GIF", gif))
            if (live > 0) add(MediaFilterCount(MediaKind.LivePhoto, FrameStrings.mediaLive(), live))
            if (motion > 0) add(MediaFilterCount(MediaKind.MotionPhoto, FrameStrings.mediaMotion(), motion))
            if (animated > 0) add(MediaFilterCount(MediaKind.Animated, FrameStrings.mediaAnimated(), animated))
        }
        return MediaSummary(albumList, filters)
    }

    fun clearSummaryCache() {
        FolderMediaIndex.invalidate()
        cached = null
    }

    private fun scanSummary(folder: List<FolderEntry>, consumer: (MediaItem) -> Unit) {
        val liveKeys = linkedSetOf<String>()
        val videos = ArrayList<MediaItem>()
        if (PermissionHelper.canReadVideos(context)) {
            MediaStoreMediaQuery.queryCursor(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStoreMediaQuery.videoProjection, null, true) { item ->
                videos += item
                if (item.durationMs in 0..LivePhotoMatcher.MAX_CLIP_MS) LivePhotoMatcher.videoPairKey(item.bucketId, item.displayName)?.let { liveKeys += it }
            }
        }
        val pairedKeys = hashSetOf<String>()
        if (PermissionHelper.canReadImages(context)) {
            val liveBases = liveKeys + LivePhotoMatcher.heifMarkers(context, liveKeys)
            MediaStoreMediaQuery.queryCursor(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStoreMediaQuery.imageProjection, null, false, liveBases, MediaStoreMediaQuery.specialImages) { item ->
                if (item.kind == MediaKind.LivePhoto) pairedKeys += LivePhotoMatcher.keyOf(item.bucketId, item.displayName)
                consumer(item)
            }
        }
        videos.forEach { item ->
            val hidden = item.displayName.endsWith(".mov", true) && item.durationMs in 0..LivePhotoMatcher.MAX_CLIP_MS && LivePhotoMatcher.keyOf(item.bucketId, item.displayName) in pairedKeys
            if (!hidden) consumer(item)
        }
        folder.forEach { consumer(it.toMediaItem()) }
    }
}
