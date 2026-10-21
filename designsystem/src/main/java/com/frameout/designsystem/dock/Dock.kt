package com.frameout.designsystem.dock

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.frameHoldRepeat
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.motion.LaunchOrigin
import com.frameout.designsystem.motion.OriginAnchor
import com.frameout.designsystem.motion.anchorOrigin
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.motion.rememberOriginAnchor
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.FrameWindowSize
import com.frameout.designsystem.theme.frameWindowSize

private val DockSlot = 48.dp

@Composable
fun FrameDock(
    items: List<DockItem>,
    modifier: Modifier = Modifier,
    fab: (@Composable () -> Unit)? = null,
    collapsed: Boolean = false,
    compactLimit: Int = 4,
    fabLabel: String? = null
) {
    val moreLabel = LocalDockMoreLabel.current
    BoxWithConstraints(modifier = modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        val islandMaxWidth = maxWidth
        val size = frameWindowSize(islandMaxWidth)
        val dockScale by animateFloatAsState(if (collapsed) 0.84f else 1f, frameSpring(), label = "dockCollapseScale")
        val dockAlpha by animateFloatAsState(if (collapsed) 0.72f else 1f, frameSpring(), label = "dockCollapseAlpha")
        if (size == FrameWindowSize.Compact) {
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            val lift by animateDpAsState(if (fab != null) IslandFabLift else 0.dp, frameSpring(), label = "dockFabLift")
            Column(
                Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(lift))
                val visible = withOverflow(items, compactLimit, moreLabel)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    FrameIsland(visible, fab, fabLabel, islandMaxWidth, rtl, Modifier.scale(dockScale).alpha(dockAlpha))
                }
            }
        } else {
            Box(Modifier.align(Alignment.CenterEnd)) {
                FrameSideRail(withOverflow(items, 6, moreLabel), size == FrameWindowSize.Expanded, fab, Modifier.alpha(dockAlpha))
            }
        }
    }
}

@Composable
internal fun DockIconButton(item: DockItem) {
    val colors = FrameTheme.colors
    val selectedT by animateFloatAsState(if (item.selected) 1f else 0f, frameSpring(), label = "dockSelected")
    val halo = if (item.destructive) colors.error else colors.primary
    val tintTarget = when {
        item.destructive -> colors.error
        item.selected -> colors.primary
        else -> colors.onSurfaceVariant
    }
    val tint by animateColorAsState(tintTarget, frameSpring(), label = "dockTint")
    val icon = if (item.selected) item.selectedIcon else item.icon
    var tip by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val anchor = rememberOriginAnchor()
    Box(
        modifier = Modifier
            .size(DockSlot)
            .anchorOrigin(anchor)
            .graphicsLayer {
                val s = 1f + 0.06f * selectedT
                scaleX = s
                scaleY = s
            }
            .drawBehind { drawDockHalo(halo, selectedT) }
            .then(dockPress(item, anchor, colors.primary, { tip = true }, { menu = true }))
            .semantics { selected = item.selected },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = item.label,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        if (tip && !menu) DockTooltip(item.label) { tip = false }
        if (menu) DockOverflowMenu(item.overflow) { menu = false }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDockHalo(halo: Color, t: Float) {
    if (t <= 0.01f) return
    val inset = 4.dp.toPx()
    val tl = Offset(inset, inset)
    val sz = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
    val radius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx())
    drawRoundRect(Brush.verticalGradient(listOf(halo.copy(alpha = 0.18f * t), halo.copy(alpha = 0.10f * t))), tl, sz, radius)
    drawRoundRect(halo.copy(alpha = 0.22f * t), tl, sz, radius, style = Stroke(1.dp.toPx()))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun dockPress(item: DockItem, anchor: OriginAnchor, tint: Color, onLongPress: () -> Unit, onOverflow: () -> Unit): Modifier {
    val haptics = rememberFrameHaptics()
    if (item.repeat) return item.holdModifier()
    return Modifier.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        role = Role.Button,
        onLongClick = {
            haptics.perform(FrameHaptic.Select)
            onLongPress()
        },
        onClick = {
            haptics.perform(if (item.selected) FrameHaptic.Select else FrameHaptic.Tap)
            if (item.overflow.isNotEmpty()) onOverflow() else { LaunchOrigin.mark(anchor, tint); item.onClick() }
        }
    )
}
