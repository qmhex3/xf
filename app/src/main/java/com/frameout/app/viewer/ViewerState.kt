package com.frameout.app.viewer

import android.graphics.Bitmap
import com.frameout.core.db.FrameEntity
import com.frameout.core.edit.FrameEdit

data class ViewerState(
    val previous: FrameEntity? = null,
    val current: FrameEntity? = null,
    val next: FrameEntity? = null,
    val previousBitmap: Bitmap? = null,
    val bitmap: Bitmap? = null,
    val placeholder: Bitmap? = null,
    val placeholderSharp: Boolean = false,
    val nextBitmap: Bitmap? = null,
    val loading: Boolean = true,
    val edit: FrameEdit = FrameEdit(0),
    val cropL: Float = 0f,
    val cropT: Float = 0f,
    val cropR: Float = 1f,
    val cropB: Float = 1f,
    val sourceW: Int = 0,
    val sourceH: Int = 0,
    val controlsVisible: Boolean = true,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val playing: Boolean = false,
    val playFps: Int = 10,
    val message: String? = null,
    val busyMessage: String? = null,
    val toolBusy: Boolean = false,
    val messageSaved: com.frameout.designsystem.icons.SaveKind? = null,
    @androidx.annotation.DrawableRes val messageIcon: Int? = null,
    val deletedStack: List<Long> = emptyList(),
    val tool: EditTool = EditTool.None,
    val draft: EditDraft = EditDraft(),
    val panelPx: Int = 0,
    val detections: List<Detection> = emptyList(),
    val showDetections: Boolean = false,
    val showPixelGrid: Boolean = false,
    val liveText: LiveTextState = LiveTextState(),
    val position: Int = 0,
    val total: Int = 0,
    val savedMode: Boolean = false,
    val savedUndoCount: Int = 0,
    val savedConsent: SavedConsent? = null,
    val discardPrompt: Boolean = false
) {
    val canGoPrevious: Boolean
        get() = total == 0 || position > 1

    val canGoNext: Boolean
        get() = total == 0 || position < total

    val hasUndo: Boolean
        get() = deletedStack.isNotEmpty() || savedUndoCount > 0

    val editing: Boolean
        get() = tool != EditTool.None

    val messageBusy: Boolean
        get() = message != null && message == busyMessage

    val cropMode: Boolean
        get() = tool == EditTool.Crop

    val hasCrop: Boolean
        get() = cropL > 0.001f || cropT > 0.001f || cropR < 0.999f || cropB < 0.999f
}

data class Detection(val box: com.frameout.core.quality.NormBox, val tags: List<String>)

val PLAY_SPEEDS = listOf(2, 5, 10, 15, 24, 30)

fun nextPlayFps(current: Int): Int = PLAY_SPEEDS.firstOrNull { it > current } ?: PLAY_SPEEDS.first()
