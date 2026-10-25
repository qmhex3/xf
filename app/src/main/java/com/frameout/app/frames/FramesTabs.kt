package com.frameout.app.frames

import com.frameout.designsystem.glass.frameGlow
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.sp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline

private val TabsPillHeight = 44.dp
private val TabsVerticalPad = 4.dp

@Composable
fun FramesTabsSlot(visible: Boolean, current: FrameFilter, all: Int, favorites: Int, sharpest: Int, items: Int, onSelect: (FrameFilter) -> Unit) {
    if (visible) FramesTabs(current, all, favorites, sharpest, items, onSelect)
    else Spacer(Modifier.fillMaxWidth().height(TabsPillHeight + TabsVerticalPad * 2))
}

@Composable
fun FramesTabs(current: FrameFilter, all: Int, favorites: Int, sharpest: Int, items: Int, onSelect: (FrameFilter) -> Unit) {
    val colors = FrameTheme.colors
    val tabs = listOf(
        Triple(FrameFilter.All, FrameStrings.all(), all),
        Triple(FrameFilter.Favorites, FrameStrings.favorites(), favorites),
        Triple(FrameFilter.Sharpest, FrameStrings.sharpest(), sharpest),
        Triple(FrameFilter.Albums, FrameStrings.items(), items)
    )
    val index = when (current) { FrameFilter.All -> 0; FrameFilter.Favorites -> 1; FrameFilter.Sharpest -> 2; FrameFilter.Albums, FrameFilter.Tagged -> 3; FrameFilter.Deleted -> -1 }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = TabsVerticalPad)
            .height(TabsPillHeight)
            .background(colors.fieldFill, FrameShapes.pill)
            .border(1.dp, frameHairline(), FrameShapes.pill)
    ) {
        val segment = (maxWidth - 8.dp) / tabs.size
        val roomy = segment >= 92.dp
        val thumbX by animateDpAsState(segment * index.coerceAtLeast(0), frameSpring(), label = "framesTabThumb")
        if (index >= 0) Box(
            Modifier
                .padding(4.dp)
                .offset(x = thumbX)
                .width(segment)
                .fillMaxHeight()
                .frameGlow(colors.primary, { if (colors.isDark) 0.22f else 0.14f }, 10.dp)
                .clip(FrameShapes.pill)
                .background(Brush.verticalGradient(listOf(colors.primary.copy(alpha = if (colors.isDark) 0.26f else 0.14f), colors.primary.copy(alpha = if (colors.isDark) 0.16f else 0.08f))))
                .border(1.dp, colors.primary.copy(alpha = 0.40f), FrameShapes.pill)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { i, (filter, label, count) -> Tab(label, count, i == index, compact = !roomy) { onSelect(filter) } }
        }
    }
}

@Composable
private fun RowScope.Tab(label: String, count: Int, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val content by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, frameSpring(), label = "framesTabContent")
    val badge by animateColorAsState(if (selected) colors.primary.copy(alpha = 0.18f) else colors.onSurface.copy(alpha = 0.08f), frameSpring(), label = "framesTabBadge")
    Box(
        Modifier.weight(1f).fillMaxHeight().clip(FrameShapes.pill).frameSelectable(selected, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)) {
            FrameText(label, style = FrameTheme.typography.label, color = content, maxLines = 1)
            if (count >= 0) Box(Modifier.background(badge, FrameShapes.pill).padding(horizontal = if (compact) 4.dp else 5.dp, vertical = 1.dp)) {
                FrameText(
                    compactCount(count),
                    style = FrameTheme.typography.monoSmall.let { if (compact) it.copy(fontSize = 10.sp) else it },
                    color = content,
                    maxLines = 1
                )
            }
        }
    }
}

private fun compactCount(count: Int): String = when {
    count < 1000 -> count.toString()
    count < 10_000 -> "%d.%dK".format(count / 1000, count % 1000 / 100)
    else -> "${count / 1000}K"
}
