package com.frameout.core.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

object LivePhotoMatcher {
    private const val FALLBACK_LIMIT = 256
    const val MAX_CLIP_MS = 10_000L

    fun keyOf(bucketId: String, displayName: String): String = "$bucketId/${displayName.substringBeforeLast('.').lowercase()}"

    fun isHeif(mime: String, name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return mime.equals("image/heic", true) || mime.equals("image/heif", true) || ext == "heic" || ext == "heif"
    }

    fun isLiveStill(mime: String, name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return isHeif(mime, name) || mime.equals("image/jpeg", true) || ext == "jpg" || ext == "jpeg"
    }

    private const val MP4_SUFFIX = "|mp4"
    private const val HEIF_SUFFIX = "|heif"

    fun canPair(stillIsHeif: Boolean, videoExt: String): Boolean {
        val ext = videoExt.lowercase()
        return ext == "mov" || (stillIsHeif && ext == "mp4")
    }

    fun videoPairKey(bucketId: String, displayName: String): String? = when (displayName.substringAfterLast('.', "").lowercase()) {
        "mov" -> keyOf(bucketId, displayName)
        "mp4" -> keyOf(bucketId, displayName) + MP4_SUFFIX
        else -> null
    }

    fun isPairedStill(pairKeys: Set<String>, bucketId: String, displayName: String, mime: String): Boolean {
        val key = keyOf(bucketId, displayName)
        val heif = isHeif(mime, displayName)
        if (!heif && (key + HEIF_SUFFIX) in pairKeys) return false
        return key in pairKeys || (heif && (key + MP4_SUFFIX) in pairKeys)
    }

    fun heifMarkers(context: Context, videoKeys: Set<String>): Set<String> {
        val movKeys = videoKeys.filter { !it.endsWith(MP4_SUFFIX) }.toSet()
        if (movKeys.isEmpty() || !PermissionHelper.canReadImages(context)) return emptySet()
        return heifKeys(context, null, movKeys).map { it + HEIF_SUFFIX }.toSet()
    }

    private fun heifKeys(context: Context, buckets: List<String>?, wanted: Set<String>): Set<String> {
        if (wanted.isEmpty()) return emptySet()
        val found = HashSet<String>()
        val bucketClause = if (buckets.isNullOrEmpty()) "" else " AND ${MediaStore.Images.Media.BUCKET_ID} IN (${buckets.joinToString(",") { "?" }})"
        val selection = "(${MediaStore.Images.Media.MIME_TYPE} IN (?, ?) OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?)$bucketClause"
        val args = (listOf("image/heic", "image/heif", "%.heic", "%.heif") + (buckets ?: emptyList())).toTypedArray()
        runCatching {
            context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.BUCKET_ID), selection, args, null)?.use { cursor ->
                val name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val bucket = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                while (cursor.moveToNext()) {
                    val key = keyOf(cursor.getString(bucket).orEmpty(), cursor.getString(name).orEmpty())
                    if (key in wanted) found += key
                }
            }
        }
        return found
    }

    private fun hasHeifSibling(context: Context, bucket: String, base: String): Boolean {
        val selection = "${MediaStore.Images.Media.BUCKET_ID}=? AND ${MediaStore.Images.Media.DISPLAY_NAME} COLLATE NOCASE IN (?, ?)"
        val args = arrayOf(bucket, "$base.heic", "$base.heif")
        return runCatching {
            context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID), selection, args, null)?.use { it.moveToFirst() } ?: false
        }.getOrDefault(false)
    }

    fun findPair(context: Context, imageUri: Uri, displayName: String, mime: String = ""): Uri? {
        if (!PermissionHelper.canReadVideos(context)) return null
        val bucket = bucketOf(context, imageUri)
        val contentId = ContentIdentifierReader.read(context, imageUri)
        if (contentId != null) findVideoByContentId(context, contentId, bucket)?.let { return it }
        if (bucket == null) return null
        val base = displayName.substringBeforeLast('.').lowercase()
        val heif = isHeif(mime, displayName)
        if (!heif && hasHeifSibling(context, bucket, base)) return null
        return findVideoByBase(context, bucket, base, heif)
    }

    fun contentMatches(context: Context, images: List<LiveImage>): List<ContentMatch> {
        if (!PermissionHelper.canReadVideos(context) || images.isEmpty()) return emptyList()
        val wanted = images.mapNotNull { image ->
            ContentIdentifierReader.read(context, image.uri)?.let { ContentRequest(keyOf(image.bucketId, image.name), image.bucketId, it) }
        }
        if (wanted.isEmpty()) return emptyList()
        val byId = wanted.associateBy { it.contentId }
        val candidates = recentVideoCandidates(context, wanted.map { it.bucketId }.distinct())
        return candidates.mapNotNull { candidate ->
            val id = ContentIdentifierReader.read(context, candidate.uri) ?: return@mapNotNull null
            val request = byId[id] ?: return@mapNotNull null
            if (request.bucketId != candidate.bucketId) return@mapNotNull null
            ContentMatch(request.key, candidate.id)
        }
    }

    fun matchKeys(context: Context, images: List<StillRef>): Set<String> {
        if (!PermissionHelper.canReadVideos(context) || images.isEmpty()) return emptySet()
        val wanted = images.filter { keyOf(it.bucketId, it.name).substringAfter('/').isNotBlank() }.map { keyOf(it.bucketId, it.name) }.toSet()
        if (wanted.isEmpty()) return emptySet()
        val buckets = images.map { it.bucketId }.distinct()
        val placeholders = buckets.joinToString(",") { "?" }
        val selection = "${MediaStore.Video.Media.BUCKET_ID} IN ($placeholders) AND ${MediaStore.Video.Media.DURATION} <= ?"
        val args = (buckets + MAX_CLIP_MS.toString()).toTypedArray()
        val found = HashSet<String>()
        context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.BUCKET_ID), selection, args, null)?.use { cursor ->
            val name = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val bucket = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            while (cursor.moveToNext()) {
                val pair = videoPairKey(cursor.getString(bucket).orEmpty(), cursor.getString(name).orEmpty()) ?: continue
                found += pair
            }
        }
        val pending = images.filter { !isHeif(it.mime, it.name) && keyOf(it.bucketId, it.name) in found }.map { keyOf(it.bucketId, it.name) }.toSet()
        val inPage = images.filter { isHeif(it.mime, it.name) }.map { keyOf(it.bucketId, it.name) }.toSet()
        val external = heifKeys(context, buckets, pending - inPage)
        return resolvePairs(images, found, external)
    }

    internal fun resolvePairs(images: List<StillRef>, videoKeys: Set<String>, externalHeifKeys: Set<String> = emptySet()): Set<String> {
        val heifKeys = images.filter { isHeif(it.mime, it.name) }.map { keyOf(it.bucketId, it.name) }.toSet() + externalHeifKeys
        val result = linkedSetOf<String>()
        images.forEach { image ->
            val key = keyOf(image.bucketId, image.name)
            val heif = isHeif(image.mime, image.name)
            if (key in videoKeys) {
                if (heif || key !in heifKeys) result += key else result += key + HEIF_SUFFIX
            }
            if (heif && (key + MP4_SUFFIX) in videoKeys) result += key + MP4_SUFFIX
        }
        return result
    }

    fun matchImageKeys(context: Context, videos: List<LiveVideo>): Set<String> {
        if (!PermissionHelper.canReadImages(context) || videos.isEmpty()) return emptySet()
        val shortMov = videos.filter { it.durationMs in 0..MAX_CLIP_MS && it.name.endsWith(".mov", true) }
        if (shortMov.isEmpty()) return emptySet()
        val wanted = shortMov.map { keyOf(it.bucketId, it.name) }.toSet()
        val buckets = shortMov.map { it.bucketId }.distinct()
        val placeholders = buckets.joinToString(",") { "?" }
        val selection = "${MediaStore.Images.Media.BUCKET_ID} IN ($placeholders)"
        val result = linkedSetOf<String>()
        context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.BUCKET_ID), selection, buckets.toTypedArray(), null)?.use { cursor ->
            val name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mime = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val bucket = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            while (cursor.moveToNext()) {
                val fileName = cursor.getString(name).orEmpty()
                if (!isLiveStill(cursor.getString(mime).orEmpty(), fileName)) continue
                val key = keyOf(cursor.getString(bucket).orEmpty(), fileName)
                if (key in wanted) result += key
            }
        }
        return result
    }

    private fun bucketOf(context: Context, imageUri: Uri): String? =
        runCatching {
            context.contentResolver.query(imageUri, arrayOf(MediaStore.MediaColumns.BUCKET_ID), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()

    private fun findVideoByBase(context: Context, bucket: String, base: String, stillIsHeif: Boolean): Uri? {
        val names = listOf("$base.mov") + if (canPair(stillIsHeif, "mp4")) listOf("$base.mp4") else emptyList()
        val marks = names.joinToString(",") { "?" }
        val selection = "${MediaStore.Video.Media.BUCKET_ID}=? AND ${MediaStore.Video.Media.DURATION} <= ? AND ${MediaStore.Video.Media.DISPLAY_NAME} COLLATE NOCASE IN ($marks)"
        val args = (listOf(bucket, MAX_CLIP_MS.toString()) + names).toTypedArray()
        context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media._ID), selection, args, "${MediaStore.Video.Media._ID} ASC")?.use { cursor ->
            if (cursor.moveToFirst()) return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0))
        }
        return null
    }

    private fun findVideoByContentId(context: Context, contentId: String, bucket: String?): Uri? {
        val buckets = if (bucket == null) emptyList() else listOf(bucket)
        return recentVideoCandidates(context, buckets).firstOrNull { ContentIdentifierReader.read(context, it.uri) == contentId }?.uri
    }

    private fun recentVideoCandidates(context: Context, buckets: List<String>): List<VideoCandidate> {
        val result = ArrayList<VideoCandidate>(FALLBACK_LIMIT)
        val bucketClause = if (buckets.isEmpty()) "" else " AND ${MediaStore.Video.Media.BUCKET_ID} IN (${buckets.joinToString(",") { "?" }})"
        val selection = "${MediaStore.Video.Media.DURATION} <= ?$bucketClause"
        val args = (listOf(MAX_CLIP_MS.toString()) + buckets).toTypedArray()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.BUCKET_ID),
            selection, args,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC, ${MediaStore.Video.Media._ID} DESC"
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val bucket = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            while (cursor.moveToNext() && result.size < FALLBACK_LIMIT) {
                result += VideoCandidate(cursor.getLong(id), cursor.getString(bucket).orEmpty(), ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getLong(id)))
            }
        }
        return result
    }

    private data class ContentRequest(val key: String, val bucketId: String, val contentId: String)
    data class ContentMatch(val imageKey: String, val videoId: Long)
    data class StillRef(val bucketId: String, val name: String, val mime: String)
    data class LiveImage(val name: String, val bucketId: String, val uri: Uri)
    data class LiveVideo(val name: String, val bucketId: String, val durationMs: Long)
    private data class VideoCandidate(val id: Long, val bucketId: String, val uri: Uri)
}
