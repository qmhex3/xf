package com.frameout.designsystem.components

import androidx.compose.runtime.Composable
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
fun FrameConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    tone: DialogTone = if (destructive) DialogTone.Danger else DialogTone.Info,
    icon: Int? = null
) {
    FrameDialog(
        onDismiss = onDismiss,
        tone = tone,
        title = title,
        body = body,
        primary = DialogButton(confirmLabel, onConfirm),
        secondary = dismissLabel?.let { DialogButton(it, onDismiss) },
        icon = icon ?: defaultIcon(tone)
    )
}

private fun defaultIcon(tone: DialogTone): Int = when (tone) {
    DialogTone.Danger -> FrameIcons.Trash
    DialogTone.Warning -> FrameIconsExtra.Warning
    DialogTone.Success -> FrameIcons.Check
    DialogTone.Info -> FrameIcons.Info
}
