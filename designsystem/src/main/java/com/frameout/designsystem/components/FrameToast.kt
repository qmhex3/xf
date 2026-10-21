package com.frameout.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.icons.SaveKind

enum class ToastKind { Info, Success, Error }

class ToastAction(val label: String, val onClick: () -> Unit, val icon: Int = FrameIconsExtra.Undo)

fun toastDurationMs(message: String, kind: ToastKind = ToastKind.Info): Long {
    val base = 2800L + message.length * 45L
    return (if (kind == ToastKind.Error) base + 2500L else base).coerceAtMost(12000L)
}

@Composable
fun FrameToastHost(
    message: String?,
    modifier: Modifier = Modifier,
    kind: ToastKind = ToastKind.Info,
    icon: Int? = null,
    durationMs: Long? = null,
    actions: List<ToastAction> = emptyList(),
    onDismiss: (() -> Unit)? = null,
    busy: Boolean = false,
    saved: SaveKind? = null
) {
    val state = message?.let {
        islandToastState(
            it,
            kind,
            busy,
            actions.mapIndexed { i, a -> IslandAction(a.label, a.icon, primary = i == 0, onClick = a.onClick) },
            saved?.icon ?: icon,
            saved != null
        )
    }
    FrameIslandToast(state, modifier, message?.let { durationMs ?: toastDurationMs(it, kind) }, onDismiss)
}
