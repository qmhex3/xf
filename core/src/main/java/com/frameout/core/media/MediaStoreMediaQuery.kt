package com.frameout.core.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

object MediaStoreMediaQuery {
    class Filter(val selection: String, val args: List<String>)

    private val specialMimes = listOf("image/gif", "image/heic", "image/heif", "image/webp", "image/apng")
    private val specialNames = listOf("mvimg%", "%.mp.%", "%\\_mp.jpg", "%.apng")

    private val motionExtraDirs = listOf("Pictures", "Download")
    private val pngDirs = listOf("Pictures", "Download")

    @android.annotation.SuppressLint("InlinedApi")
    internal fun buildSpecialImages(sdk: Int): Filter {
        val mime = MediaStore.MediaColumns.MIME_TYPE
        val size = MediaStore.MediaColumns.SIZE
        val clauses = ArrayList<String>()
        val args = ArrayList<String>()
        specialMimes.forEach { clauses += "$mime=?"; args += it }
        specialNames.forEach { clauses += "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? ESCAPE '\\'"; args += it }

        val pathColumn = if (sdk >= 29) MediaStore.MediaColumns.RELATIVE_PATH else @Suppress("DEPRECATION") MediaStore.MediaColumns.DATA
        fun dir(name: String) = if (sdk >= 29) "$name/%" else "%/$name/%"
        fun anyOf(names: List<String>) = names.joinToString(" OR ") { "$pathColumn LIKE ?" }

        clauses += "($mime=? AND $pathColumn LIKE ?)"
        args += listOf("image/jpeg", dir("DCIM"))

        clauses += "($mime=? AND $size>=? AND (${anyOf(motionExtraDirs)}))"
        args += listOf("image/jpeg", GalleryProbeRules.MOTION_EXTRA_MIN_BYTES.toString()) + motionExtraDirs.map(::dir)

        val noScreens = if (sdk >= 29) "($pathColumn IS NULL OR $pathColumn NOT LIKE ?)" else "$pathColumn NOT LIKE ?"
        clauses += "($mime=? AND $size>=? AND $size<=? AND (${anyOf(pngDirs)}) AND $noScreens)"
        args += listOf("image/png", GalleryProbeRules.PNG_MIN_BYTES.toString(), GalleryProbeRules.PNG_MAX_BYTES.toString()) + pngDirs.map(::dir) + "%Screenshot%"

        return Filter(clauses.joinToString(" OR "), args)
    }

    val specialImages = buildSpecialImages(android.os.Build.VERSION.SDK_INT)

    val videoProjection = arrayOf(
        MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.BUCKET_ID,
        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT,
        MediaStore.Video.Media.DURATION
    )
    val imageProjection = arrayOf(
        MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.BUCKET_ID,
        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT
    )

    fun query(context: Context, collection: Uri, projection: Array<String>, bucketId: String?, isVideo: Boolean, liveBases: Set<String> = emptySet()): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val selection = if (bucketId != null) "${MediaStore.MediaColumns.BUCKET_ID}=?" else null
        val args = bucketId?.let { arrayOf(it) }
        context.contentResolver.query(collection, projection, selection, args, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val durationCol = if (isVideo) cursor.getColumnIndex(MediaStore.Video.Media.DURATION) else -1
            while (cursor.moveToNext()) addItem(context, cursor, result, collection, isVideo, liveBases, idCol, nameCol, mimeCol, sizeCol, dateCol, bucketIdCol, bucketNameCol, widthCol, heightCol, durationCol)
        }
        return result
    }

    fun queryCursor(context: Context, collection: Uri, projection: Array<String>, bucketId: String?, isVideo: Boolean, consumer: (MediaItem) -> Unit) = queryCursor(context, collection, projection, bucketId, isVideo, emptySet(), null, consumer)

    fun queryCursor(context: Context, collection: Uri, projection: Array<String>, bucketId: String?, isVideo: Boolean, liveBases: Set<String>, filter: Filter? = null, consumer: (MediaItem) -> Unit) {
        val clauses = ArrayList<String>(2)
        val arguments = ArrayList<String>()
        if (bucketId != null) {
            clauses += "${MediaStore.MediaColumns.BUCKET_ID}=?"
            arguments += bucketId
        }
        if (filter != null) {
            clauses += "(${filter.selection})"
            arguments += filter.args
        }
        val selection = if (clauses.isEmpty()) null else clauses.joinToString(" AND ")
        val args = if (arguments.isEmpty()) null else arguments.toTypedArray()
        context.contentResolver.query(collection, projection, selection, args, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val durationCol = if (isVideo) cursor.getColumnIndex(MediaStore.Video.Media.DURATION) else -1
            while (cursor.moveToNext()) buildItem(context, filter != null, cursor, collection, isVideo, liveBases, idCol, nameCol, mimeCol, sizeCol, dateCol, bucketIdCol, bucketNameCol, widthCol, heightCol, durationCol)?.takeIf { stillAnimated(context, it) }?.let(consumer)
        }
        ProbeStore.flushAll()
    }

    private fun addItem(context: Context, cursor: android.database.Cursor, result: MutableList<MediaItem>, collection: Uri, isVideo: Boolean, liveBases: Set<String>, idCol: Int, nameCol: Int, mimeCol: Int, sizeCol: Int, dateCol: Int, bucketIdCol: Int, bucketNameCol: Int, widthCol: Int, heightCol: Int, durationCol: Int) {
        buildItem(context, false, cursor, collection, isVideo, liveBases, idCol, nameCol, mimeCol, sizeCol, dateCol, bucketIdCol, bucketNameCol, widthCol, heightCol, durationCol)?.takeIf { stillAnimated(context, it) }?.let { result += it }
    }

    private fun buildItem(context: Context, probeMotion: Boolean, cursor: android.database.Cursor, collection: Uri, isVideo: Boolean, liveBases: Set<String>, idCol: Int, nameCol: Int, mimeCol: Int, sizeCol: Int, dateCol: Int, bucketIdCol: Int, bucketNameCol: Int, widthCol: Int, heightCol: Int, durationCol: Int): MediaItem? {
        val name = cursor.getString(nameCol) ?: ""
        val mime = cursor.getString(mimeCol) ?: ""
        val bucket = cursor.getString(bucketIdCol) ?: ""
        var kind = resolveKind(mime, isVideo, name, bucket, liveBases)
        val id = cursor.getLong(idCol)
        if (kind == MediaKind.Image && probeMotion && !isVideo && MotionPhotoExtractor.isMotionCandidate(mime, name) &&
            MotionPhotoExtractor.isMotionPhotoQuick(context, ContentUris.withAppendedId(collection, id))
        ) kind = MediaKind.MotionPhoto
        if (kind == MediaKind.Image) return null
        return MediaItem(id, ContentUris.withAppendedId(collection, id).toString(), name, mime, cursor.getLong(sizeCol), if (durationCol >= 0) cursor.getLong(durationCol) else 0L, if (widthCol >= 0) cursor.getInt(widthCol) else 0, if (heightCol >= 0) cursor.getInt(heightCol) else 0, cursor.getLong(dateCol), cursor.getString(bucketIdCol) ?: "", cursor.getString(bucketNameCol) ?: "", kind)
    }

    private fun stillAnimated(context: Context, item: MediaItem): Boolean =
        item.kind != MediaKind.Animated || AnimatedImageProbe.isAnimated(context, Uri.parse(item.uri), item.mimeType, item.displayName, item.sizeBytes, item.dateAdded)

    private fun resolveKind(mime: String, isVideo: Boolean, name: String, bucket: String, liveBases: Set<String>): MediaKind {
        val lower = mime.lowercase()
        return when {
            lower == "image/gif" -> MediaKind.Gif
            lower.contains("image") && lower.contains("motion") -> MediaKind.MotionPhoto
            MotionPhotoExtractor.isLikelyMotionName(name) -> MediaKind.MotionPhoto
            !isVideo && (AnimatedImageProbe.isCandidate(lower, name) || AnimatedImageProbe.isPngCandidate(lower, name)) -> MediaKind.Animated
            !isVideo && LivePhotoMatcher.isLiveStill(mime, name) && LivePhotoMatcher.isPairedStill(liveBases, bucket, name, mime) -> MediaKind.LivePhoto
            isVideo -> MediaKind.Video
            else -> MediaKind.Image
        }
    }
}
