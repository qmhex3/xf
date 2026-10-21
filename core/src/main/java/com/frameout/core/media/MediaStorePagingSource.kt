package com.frameout.core.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStorePagingSource(private val context: Context, private val bucketId: String?, private val kind: MediaKind?) : PagingSource<MediaKey, MediaItem>() {
    private var folder: List<FolderEntry>? = null

    override suspend fun load(params: LoadParams<MediaKey>): LoadResult<MediaKey, MediaItem> = try {
        val entries = folderEntries()
        val page = withContext(Dispatchers.IO) { query(params.key, params.loadSize, entries) }
        LoadResult.Page(page.items, null, page.next)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LoadResult.Error(e)
    }

    private suspend fun folderEntries(): List<FolderEntry> {
        folder?.let { return it }
        val skip = kind == MediaKind.LivePhoto || kind == MediaKind.MotionPhoto
        val loaded = if (skip) emptyList() else FolderMediaIndex.loadSafely(context)
        val scoped = loaded.filter { (bucketId == null || it.bucketId == bucketId) && (kind == null || it.kind == kind) }
        folder = scoped
        return scoped
    }

    private fun query(key: MediaKey?, limit: Int, entries: List<FolderEntry>): PageResult {
        var cursorKey = key
        var attempts = 0
        while (true) {
            val result = queryOnce(cursorKey, limit, entries)
            val next = result.next
            if (result.items.isNotEmpty() || next == null || ++attempts >= MAX_EMPTY_RETRIES) return result
            cursorKey = next
        }
    }

    private fun queryOnce(key: MediaKey?, limit: Int, entries: List<FolderEntry>): PageResult {
        val scan = openCursor(key, limit)?.use { readRows(it) } ?: RawScan(emptyList(), null, 0)
        val merge = PageMerge.plan(scan.lastKey, scan.rawCount >= limit, entries, key, limit) { it.key }
        val rows = scan.rows.filter { merge.accepts(MediaKey(it.date, it.id)) }
        val stored = buildItems(rows)
        val extra = merge.folder.map { it.toMediaItem() }
        val ordered = (stored + extra).sortedWith(compareByDescending<MediaItem> { it.dateAdded }.thenByDescending { it.id })
        ProbeStore.flushAll()
        return PageResult(ordered, merge.next)
    }

    private fun openCursor(key: MediaKey?, limit: Int): android.database.Cursor? {
        val uri = MediaStore.Files.getContentUri("external")
        val args = arrayListOf<String>()
        val selection = buildSelection(args, key)
        val sort = "${MediaStore.MediaColumns.DATE_ADDED} DESC, ${MediaStore.MediaColumns._ID} DESC"
        val projection = mediaProjection()
        if (Build.VERSION.SDK_INT < 26) return context.contentResolver.query(uri, projection, selection, args.toTypedArray(), "$sort LIMIT $limit")
        val extras = Bundle().apply {
            putInt(QueryArgs.LIMIT, limit)
            putInt(QueryArgs.OFFSET, 0)
            putString(QueryArgs.SORT, sort)
            putString(QueryArgs.SELECTION, selection)
            putStringArray(QueryArgs.SELECTION_ARGS, args.toTypedArray())
        }
        return context.contentResolver.query(uri, projection, extras, null)
    }

    private fun mediaProjection() = arrayOf(
        MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.BUCKET_ID, MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
        MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT, MediaStore.Video.Media.DURATION, MediaStore.Files.FileColumns.MEDIA_TYPE
    )

    private fun buildItems(rows: List<Row>): List<MediaItem> {
        val liveRows = rows.filter { it.kind == MediaKind.LivePhoto }
        val liveKeys = LivePhotoMatcher.matchKeys(context, liveRows.map { LivePhotoMatcher.StillRef(it.bucket, it.name, it.mime) })
        val contentMatches = LivePhotoMatcher.contentMatches(context, liveRows.filter { LivePhotoMatcher.isHeif(it.mime, it.name) }.map { row -> LivePhotoMatcher.LiveImage(row.name, row.bucket, ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, row.id)) })
        val contentKeys = contentMatches.map { it.imageKey }.toSet()
        val pairedVideoIds = contentMatches.map { it.videoId }.toSet()
        val pairedVideos = LivePhotoMatcher.matchImageKeys(context, rows.filter { it.kind == MediaKind.Video }.map { LivePhotoMatcher.LiveVideo(it.name, it.bucket, it.duration) })
        return rows.mapNotNull { toMediaItem(it, liveKeys + contentKeys, pairedVideos, pairedVideoIds) }
    }

    private fun readRows(c: android.database.Cursor): RawScan {
        val x = cursorColumns(c)
        val rows = ArrayList<Row>(c.count)
        var lastKey: MediaKey? = null
        while (c.moveToNext()) {
            val name = c.getString(x[1]).orEmpty()
            val mime = c.getString(x[2]).orEmpty()
            lastKey = MediaKey(c.getLong(x[4]), c.getLong(x[0]))
            val type = resolveKind(name, mime, c.getInt(x[10])) ?: continue
            rows += Row(c.getLong(x[0]), name, mime, c.getLong(x[3]), c.getLong(x[4]), c.getString(x[5]).orEmpty(), c.getString(x[6]).orEmpty(), if (x[7] >= 0) c.getInt(x[7]) else 0, if (x[8] >= 0) c.getInt(x[8]) else 0, if (x[9] >= 0) c.getLong(x[9]) else 0L, c.getInt(x[10]), type)
        }
        return RawScan(rows, lastKey, c.count)
    }

    private fun cursorColumns(c: android.database.Cursor) = intArrayOf(
        c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID), c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME),
        c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE), c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE),
        c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED), c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID),
        c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME), c.getColumnIndex(MediaStore.MediaColumns.WIDTH),
        c.getColumnIndex(MediaStore.MediaColumns.HEIGHT), c.getColumnIndex(MediaStore.Video.Media.DURATION), c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
    )

    private fun toMediaItem(row: Row, liveBases: Set<String>, pairedVideos: Set<String>, pairedVideoIds: Set<Long>): MediaItem? {
        if (row.kind == MediaKind.Video && (row.id in pairedVideoIds || (row.name.endsWith(".mov", true) && row.duration in 0..LivePhotoMatcher.MAX_CLIP_MS && LivePhotoMatcher.keyOf(row.bucket, row.name) in pairedVideos))) return null
        val collection = if (row.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val itemUri = ContentUris.withAppendedId(collection, row.id)
        val finalKind = when {
            row.kind != MediaKind.LivePhoto -> row.kind
            LivePhotoMatcher.isPairedStill(liveBases, row.bucket, row.name, row.mime) -> MediaKind.LivePhoto
            MotionPhotoExtractor.isLikelyMotionName(row.name) -> MediaKind.MotionPhoto
            MotionPhotoExtractor.isMotionPhotoQuick(context, itemUri) -> MediaKind.MotionPhoto
            else -> return null
        }
        if (kind != null && !matches(kind, finalKind)) return null
        if (finalKind == MediaKind.Animated && !AnimatedImageProbe.isAnimated(context, itemUri, row.mime, row.name, row.size, row.date)) return null
        return MediaItem(row.id, itemUri.toString(), row.name, row.mime, row.size, row.duration, row.width, row.height, row.date, row.bucket, row.bucketName, finalKind)
    }

    private fun buildSelection(args: MutableList<String>, key: MediaKey?): String {
        val video = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        val image = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        val imageArgs = MediaStoreMediaQuery.specialImages.args
        val imageKinds = "(${MediaStoreMediaQuery.specialImages.selection})"
        args += video.toString()
        args += image.toString()
        args += imageArgs
        val bucket = bucketId?.let { args += it; " AND ${MediaStore.MediaColumns.BUCKET_ID}=?" } ?: ""
        val cursor = key?.let {
            args += it.dateAdded.toString()
            args += it.dateAdded.toString()
            args += it.id.toString()
            " AND (${MediaStore.MediaColumns.DATE_ADDED} < ? OR (${MediaStore.MediaColumns.DATE_ADDED} = ? AND ${MediaStore.MediaColumns._ID} < ?))"
        } ?: ""
        return "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR (${MediaStore.Files.FileColumns.MEDIA_TYPE}=? AND $imageKinds))$bucket$cursor"
    }

    private fun resolveKind(name: String, mime: String, mediaType: Int): MediaKind? = when {
        mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaKind.Video
        mime.equals("image/gif", true) -> MediaKind.Gif
        MotionPhotoExtractor.isLikelyMotionName(name) -> MediaKind.MotionPhoto
        AnimatedImageProbe.isCandidate(mime, name) || AnimatedImageProbe.isPngCandidate(mime, name) -> MediaKind.Animated
        LivePhotoMatcher.isLiveStill(mime, name) -> MediaKind.LivePhoto
        else -> null
    }

    private fun matches(selected: MediaKind, actual: MediaKind): Boolean = selected == actual

    override fun getRefreshKey(state: PagingState<MediaKey, MediaItem>): MediaKey? = null

    private companion object {
        const val MAX_EMPTY_RETRIES = 8
    }

    private data class Row(val id: Long, val name: String, val mime: String, val size: Long, val date: Long, val bucket: String, val bucketName: String, val width: Int, val height: Int, val duration: Long, val mediaType: Int, val kind: MediaKind)
    private data class RawScan(val rows: List<Row>, val lastKey: MediaKey?, val rawCount: Int)
    private data class PageResult(val items: List<MediaItem>, val next: MediaKey?)
}

private object QueryArgs {
    const val LIMIT = "android:query-arg-limit"
    const val OFFSET = "android:query-arg-offset"
    const val SORT = "android:query-arg-sql-sort-order"
    const val SELECTION = "android:query-arg-sql-selection"
    const val SELECTION_ARGS = "android:query-arg-sql-selection-args"
}
