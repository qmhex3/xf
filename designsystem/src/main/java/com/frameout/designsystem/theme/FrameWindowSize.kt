package com.frameout.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class FrameWindowSize { Compact, Medium, Expanded }

fun frameWindowSize(width: Dp): FrameWindowSize = when {
    width >= 840.dp -> FrameWindowSize.Expanded
    width >= 600.dp -> FrameWindowSize.Medium
    else -> FrameWindowSize.Compact
}
