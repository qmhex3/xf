package com.frameout.app.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline

@Composable
fun LibraryTabBar(tab: LibraryTab, projectCount: Int, savedCount: Int, onSelect: (LibraryTab) -> Unit) {
    val colors = FrameTheme.colors
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp)
            .background(colors.fieldFill, FrameShapes.pill)
            .border(1.dp, frameHairline(), FrameShapes.pill)
    ) {
        val segment = (maxWidth - 8.dp) / 2
        val thumbX by animateDpAsState(if (tab == LibraryTab.Projects) 0.dp else segment, frameSpring(), label = "tabThumb")
        Box(
            Modifier
                .padding(4.dp)
                .offset(x = thumbX)
                .width(segment)
                .fillMaxHeight()
                .clip(FrameShapes.pill)
                .background(Brush.verticalGradient(listOf(colors.primary.copy(alpha = if (colors.isDark) 0.26f else 0.14f), colors.primary.copy(alpha = if (colors.isDark) 0.16f else 0.08f))))
                .border(1.dp, colors.primary.copy(alpha = 0.40f), FrameShapes.pill)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            TabChip(FrameStrings.projects(), projectCount, tab == LibraryTab.Projects) { onSelect(LibraryTab.Projects) }
            TabChip(FrameStrings.saved(), savedCount, tab == LibraryTab.Saved) { onSelect(LibraryTab.Saved) }
        }
    }
}

@Composable
private fun RowScope.TabChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val content by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, frameSpring(), label = "tabContent")
    val badge by animateColorAsState(if (selected) colors.primary.copy(alpha = 0.18f) else colors.onSurface.copy(alpha = 0.08f), frameSpring(), label = "tabBadge")
    Box(
        Modifier.weight(1f).fillMaxHeight().clip(FrameShapes.pill).frameSelectable(selected, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FrameText(label, style = FrameTheme.typography.label, color = content, maxLines = 1)
            Box(Modifier.background(badge, FrameShapes.pill).padding(horizontal = 8.dp, vertical = 2.dp)) {
                FrameText(count.toString(), style = FrameTheme.typography.monoSmall, color = content)
            }
        }
    }
}
