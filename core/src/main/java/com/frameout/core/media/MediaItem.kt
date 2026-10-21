package com.frameout.core.media

import com.frameout.core.util.FrameStrings

data class MediaItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val bucketId: String,
    val bucketName: String,
    val kind: MediaKind
)

enum class MediaKind {
    Video,
    Gif,
    LivePhoto,
    MotionPhoto,
    Animated,
    Image
}

data class MediaAlbum(
    val bucketId: String,
    val name: String,
    val count: Int
)

data class MediaFilterCount(
    val kind: MediaKind?,
    val label: String,
    val count: Int
)

data class MediaSummary(
    val albums: List<MediaAlbum>,
    val filters: List<MediaFilterCount>
)

private object MediaKindLabels {
    fun all() = FrameStrings.allMedia()
    fun video() = FrameStrings.mediaVideo()
    fun gif() = "GIF"
    fun live() = FrameStrings.mediaLive()
}
