package com.frameout.app.viewer

import android.graphics.Bitmap
import com.frameout.core.db.FrameEntity
import com.frameout.core.edit.FrameEdit

internal fun ViewerState.cleared() = copy(
    current = null, bitmap = null, placeholder = null, placeholderSharp = false,
    previous = null, next = null, previousBitmap = null, nextBitmap = null,
    loading = false, scale = 1f, offsetX = 0f, offsetY = 0f, tool = EditTool.None,
    draft = EditDraft(), panelPx = 0, sourceW = 0, sourceH = 0, toolBusy = false
)

internal fun ViewerState.showing(frame: FrameEntity, edit: FrameEdit, ready: Bitmap?, placeholder: Bitmap?, sharp: Boolean) =
    withEdit(edit).copy(
        current = frame, bitmap = ready, placeholder = placeholder, placeholderSharp = sharp,
        loading = ready == null && placeholder == null,
        previous = null, next = null, previousBitmap = null, nextBitmap = null,
        scale = 1f, offsetX = 0f, offsetY = 0f, tool = EditTool.None, detections = emptyList(),
        draft = EditDraft(), panelPx = 0, sourceW = 0, sourceH = 0, toolBusy = false
    )
