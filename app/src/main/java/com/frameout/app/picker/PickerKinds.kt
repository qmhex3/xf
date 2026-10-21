package com.frameout.app.picker

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import com.frameout.core.media.MediaKind
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra

@DrawableRes
internal fun MediaKind?.icon(): Int = when (this) {
    null -> FrameIcons.GridSize
    MediaKind.Video -> FrameIconsExtra.Clip
    MediaKind.Gif -> FrameIconsExtra.Gif
    MediaKind.LivePhoto -> FrameIconsExtra.Live
    MediaKind.MotionPhoto -> FrameIconsExtra.Motion
    MediaKind.Animated -> FrameIconsExtra.Animated
    MediaKind.Image -> FrameIconsExtra.ImageFormat
}

@Composable
internal fun MediaKind?.label(): String = when (this) {
    null -> FrameStrings.allMedia()
    MediaKind.Video -> FrameStrings.mediaVideo()
    MediaKind.Gif -> "GIF"
    MediaKind.LivePhoto -> FrameStrings.mediaLive()
    MediaKind.MotionPhoto -> FrameStrings.mediaMotion()
    MediaKind.Animated -> FrameStrings.mediaAnimated()
    MediaKind.Image -> FrameStrings.allMedia()
}
