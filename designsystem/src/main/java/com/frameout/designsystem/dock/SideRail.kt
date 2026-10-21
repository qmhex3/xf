package com.frameout.designsystem.dock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.frameHoldRepeat
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.theme.FrameRailCompactWidth
import com.frameout.designsystem.theme.FrameRailExpandedWidth
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.motion.LaunchOrigin
import com.frameout.designsystem.motion.anchorOrigin
import com.frameout.designsystem.motion.rememberOriginAnchor
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun FrameSideRail(items: List<DockItem>, expanded: Boolean, fab: (@Composable () -> Unit)?, modifier: Modifier = Modifier) {
    GlassSurface(shape = FrameShapes.lg, modifier = modifier.fillMaxHeight().width(if (expanded) FrameRailExpandedWidth else FrameRailCompactWidth)) {
        Column(
            Modifier.fillMaxSize().padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { RailEntry(it, expanded) }
            Spacer(Modifier.weight(1f))
            fab?.invoke()
        }
    }
}

@Composable
private fun RailIcon(item: DockItem) {
    val colors = FrameTheme.colors
    val tint = if (item.destructive) colors.error else if (item.selected) colors.primary else colors.onSurfaceVariant
    Image(
        painter = painterResource(if (item.selected) item.selectedIcon else item.icon),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        colorFilter = ColorFilter.tint(tint)
    )
}

@Composable
private fun RailEntry(item: DockItem, expanded: Boolean) {
    if (!expanded) {
        DockIconButton(item)
        return
    }
    val colors = FrameTheme.colors
    var menu by remember { mutableStateOf(false) }
    val anchor = rememberOriginAnchor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .anchorOrigin(anchor)
            .background(if (item.selected) colors.containerHigh else colors.container.copy(alpha = 0f), FrameShapes.pill)
            .then(if (item.repeat) item.holdModifier() else Modifier.frameSelectable(item.selected, Role.Button) { if (item.overflow.isNotEmpty()) menu = true else { LaunchOrigin.mark(anchor, colors.primary); item.onClick() } })
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RailIcon(item)
        FrameText(item.label, style = FrameTheme.typography.label, color = if (item.destructive) colors.error else if (item.selected) colors.primary else colors.onSurface)
        if (menu) DockOverflowMenu(item.overflow) { menu = false }
    }
}

