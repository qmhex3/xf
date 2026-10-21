package com.frameout.designsystem.dock

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.a11y.frameHoldRepeat
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.motion.LaunchOrigin
import com.frameout.designsystem.motion.OriginAnchor
import com.frameout.designsystem.motion.anchorOrigin
import com.frameout.designsystem.motion.rememberOriginAnchor
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun IslandItem(item: DockItem, width: Dp, showOwnHalo: Boolean, entrance: Boolean, modifier: Modifier) {
    val colors = FrameTheme.colors
    var menu by remember { mutableStateOf(false) }
    val appear = remember { Animatable(if (entrance) 0f else 1f) }
    LaunchedEffect(Unit) { if (entrance) appear.animateTo(1f, islandSpring()) }
    val ownHalo by animateFloatAsState(if (item.selected && showOwnHalo) 1f else 0f, islandSpring(), label = "islandOwnHalo")
    val lift by animateFloatAsState(if (item.selected) 1.06f else 1f, islandSpring(), label = "islandLift")
    val halo = if (item.destructive) colors.error else colors.primary
    val anchor = rememberOriginAnchor()
    val press = rememberIslandPress(item, anchor, colors.primary) { menu = true }
    Box(
        modifier
            .anchorOrigin(anchor)
            .size(width, IslandBarHeight)
            .graphicsLayer {
                val s = (0.4f + 0.6f * appear.value) * lift
                scaleX = s
                scaleY = s
                alpha = appear.value.coerceIn(0f, 1f)
            }
            .drawBehind { if (ownHalo > 0.01f) drawOwnHalo(halo, ownHalo) }
            .then(press)
            .semantics(mergeDescendants = true) { selected = item.selected },
        contentAlignment = Alignment.TopCenter
    ) {
        IslandItemContent(item, width)
        if (menu) DockOverflowMenu(item.overflow) { menu = false }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberIslandPress(item: DockItem, anchor: OriginAnchor, tint: Color, openMenu: () -> Unit): Modifier {
    val haptics = rememberFrameHaptics()
    val source = remember { MutableInteractionSource() }
    return if (item.repeat) {
        item.holdModifier()
    } else {
        Modifier.combinedClickable(
            interactionSource = source,
            indication = null,
            role = Role.Button,
            onLongClick = { haptics.perform(FrameHaptic.Select) },
            onClick = {
                haptics.perform(if (item.selected) FrameHaptic.Select else FrameHaptic.Tap)
                if (item.overflow.isNotEmpty()) openMenu() else { LaunchOrigin.mark(anchor, tint); item.onClick() }
            }
        )
    }
}

@Composable
private fun IslandItemContent(item: DockItem, width: Dp) {
    val colors = FrameTheme.colors
    val iconTint by animateColorAsState(
        when {
            item.hot -> Color.White
            item.destructive -> colors.error
            item.selected -> colors.primary
            else -> colors.onSurfaceVariant
        },
        label = "islandTint"
    )
    val labelTint by animateColorAsState(
        when {
            item.destructive -> colors.error
            item.selected || item.hot -> colors.primary
            else -> colors.onSurfaceVariant
        },
        label = "islandLabelTint"
    )
    Column(Modifier.padding(top = ItemTop), horizontalAlignment = Alignment.CenterHorizontally) {
        IslandIcon(item, iconTint)
        FitLabel(item.label, labelTint, width)
    }
}

@Composable
private fun FitLabel(text: String, tint: Color, width: Dp) {
    val base = if (width < 50.dp) 10f else 11f
    var size by remember(text, width) { mutableFloatStateOf(base) }
    val atMin = size <= MinLabelSp
    FrameText(
        text,
        style = FrameTheme.typography.label.copy(fontSize = size.sp, lineHeight = 14.sp),
        color = tint,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = if (atMin) TextOverflow.Ellipsis else TextOverflow.Clip,
        softWrap = false,
        modifier = Modifier.width(width).padding(top = 2.dp, start = 1.dp, end = 1.dp),
        onTextLayout = { if (it.didOverflowWidth && !atMin) size = (size - 0.5f).coerceAtLeast(MinLabelSp) }
    )
}

private const val MinLabelSp = 8f

@Composable
private fun IslandIcon(item: DockItem, tint: Color) {
    val chip = FrameGradients.Fab
    Box(
        Modifier
            .size(46.dp, 22.dp)
            .drawBehind { if (item.hot) drawHotChip(chip) },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(if (item.selected) item.selectedIcon else item.icon),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}

private fun DrawScope.drawOwnHalo(halo: Color, ownHalo: Float) {
    val w = minOf(56.dp.toPx(), size.width - 4.dp.toPx())
    val h = 28.dp.toPx()
    val topLeft = Offset((size.width - w) / 2f, ItemTop.toPx() - 3.dp.toPx())
    val radius = CornerRadius(16.dp.toPx())
    drawRoundRect(
        Brush.verticalGradient(listOf(halo.copy(alpha = 0.18f * ownHalo), halo.copy(alpha = 0.10f * ownHalo)), topLeft.y, topLeft.y + h),
        topLeft, Size(w, h), radius
    )
    drawRoundRect(halo.copy(alpha = 0.22f * ownHalo), topLeft, Size(w, h), radius, style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawHotChip(chip: Brush) {
    val h = 28.dp.toPx()
    val top = -(h - size.height) / 2f
    val radius = CornerRadius(h / 2f)
    drawRoundRect(chip, Offset(0f, top), Size(size.width, h), radius)
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Color.Transparent, Color.Black.copy(alpha = 0.1f)), top, top + h),
        Offset(0f, top), Size(size.width, h), radius
    )
}

@Composable
internal fun IslandItemGhost(item: DockItem, width: Dp, modifier: Modifier) {
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) { fade.animateTo(0f, androidx.compose.animation.core.tween(180)) }
    Box(
        modifier
            .size(width, IslandBarHeight)
            .graphicsLayer {
                val s = 0.7f + 0.3f * fade.value
                scaleX = s
                scaleY = s
                alpha = fade.value
            },
        contentAlignment = Alignment.TopCenter
    ) { IslandItemContent(item, width) }
}
