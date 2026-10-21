package com.frameout.designsystem.dock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.frameout.designsystem.glass.framePopupSurface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private const val TOOLTIP_MS = 1600L

private class AnchoredAbove(private val gapPx: Int, private val sideways: Boolean) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        if (sideways && anchorBounds.center.x > windowSize.width * 3 / 4) {
            return IntOffset((anchorBounds.left - popupContentSize.width - gapPx).coerceIn(0, maxX), anchorBounds.top.coerceIn(0, maxY))
        }
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(0, maxX)
        val y = (anchorBounds.top - popupContentSize.height - gapPx).coerceIn(0, maxY)
        return IntOffset(x, y)
    }
}

@Composable
internal fun DockTooltip(label: String, onDismiss: () -> Unit) {
    LaunchedEffect(label) {
        delay(TOOLTIP_MS)
        onDismiss()
    }
    val gap = with(LocalDensity.current) { 10.dp.roundToPx() }
    val colors = FrameTheme.colors
    Popup(AnchoredAbove(gap, false), onDismiss, PopupProperties(focusable = false)) {
        FrameText(
            label,
            style = FrameTheme.typography.label,
            color = colors.onSurface,
            modifier = Modifier.framePopupSurface(FrameShapes.sm).padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
internal fun DockOverflowMenu(items: List<DockItem>, onDismiss: () -> Unit) {
    val gap = with(LocalDensity.current) { 12.dp.roundToPx() }
    val colors = FrameTheme.colors
    Popup(AnchoredAbove(gap, true), onDismiss, PopupProperties(focusable = true)) {
        Column(Modifier.widthIn(min = 210.dp).framePopupSurface(FrameShapes.xl).padding(vertical = 8.dp)) {
            items.forEach { item ->
                val tint = if (item.destructive) colors.error else if (item.selected) colors.primary else colors.onSurface
                Row(
                    Modifier.fillMaxWidth().height(48.dp).frameClickable { onDismiss(); item.onClick() }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(if (item.selected) item.selectedIcon else item.icon), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(tint))
                    FrameText(item.label, color = tint, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}
