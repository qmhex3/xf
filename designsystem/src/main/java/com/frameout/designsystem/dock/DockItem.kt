package com.frameout.designsystem.dock

import androidx.annotation.DrawableRes
import androidx.compose.runtime.staticCompositionLocalOf
import com.frameout.designsystem.a11y.frameHoldAccelerate
import com.frameout.designsystem.a11y.frameHoldRepeat
import com.frameout.designsystem.icons.FrameIconsExtra

data class DockItem(
    val id: String,
    val label: String,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int = icon,
    val selected: Boolean = false,
    val destructive: Boolean = false,
    val repeat: Boolean = false,
    val hot: Boolean = false,
    val overflow: List<DockItem> = emptyList(),
    val onRepeat: ((Int) -> Unit)? = null,
    val onClick: () -> Unit
)

val LocalDockMoreLabel = staticCompositionLocalOf { "More" }

internal fun withOverflow(items: List<DockItem>, limit: Int, moreLabel: String): List<DockItem> {
    if (items.size <= limit) return items
    val keep = (limit - 1).coerceAtLeast(1)
    val more = DockItem("overflow", moreLabel, FrameIconsExtra.More, overflow = items.drop(keep), onClick = {})
    return items.take(keep) + more
}

internal fun DockItem.holdModifier(): androidx.compose.ui.Modifier =
    onRepeat?.let { androidx.compose.ui.Modifier.frameHoldAccelerate(it) } ?: androidx.compose.ui.Modifier.frameHoldRepeat(onClick)
