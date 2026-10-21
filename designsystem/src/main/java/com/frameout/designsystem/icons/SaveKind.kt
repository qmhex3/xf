package com.frameout.designsystem.icons

import androidx.annotation.DrawableRes

enum class SaveKind(@DrawableRes val icon: Int) {
    Image(FrameIconsExtra.SavedImage),
    Video(FrameIconsExtra.SavedVideo),
    File(FrameIconsExtra.SavedFile);

    companion object {
        fun fromMime(mime: String?): SaveKind = when {
            mime == null -> File
            mime.startsWith("image/") -> Image
            mime.startsWith("video/") -> Video
            else -> File
        }
    }
}
