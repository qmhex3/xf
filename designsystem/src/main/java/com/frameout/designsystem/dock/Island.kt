package com.frameout.designsystem.dock

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameHoldRepeat
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

internal val IslandBarHeight = 54.dp
internal val IslandFabLift = FrameFabSize / 2 + FabRaise
private val SidePad = 12.dp
private val FabGap = 96.dp
private val MaxSlot = 62.dp
private val MinSlot = 42.dp
private val HalfFab = FrameFabSize / 2
internal val ItemTop = 6.dp
internal val IslandLabelTop = ItemTop + 24.dp

internal fun <T> islandSpring() = spring<T>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)

@Composable
internal fun FrameIsland(
    items: List<DockItem>,
    fab: (@Composable () -> Unit)?,
    fabLabel: String?,
    maxWidth: Dp,
    rtl: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = FrameTheme.colors
    val density = LocalDensity.current
    val hasFab = fab != null
    val fabHold = remember { mutableStateOf(fab) }
    if (fab != null) fabHold.value = fab
    val labelHold = remember { mutableStateOf(fabLabel) }
    if (fabLabel != null) labelHold.value = fabLabel

    val n = items.size
    val gapTarget = if (hasFab) FabGap else 0.dp
    val slotTarget = ((maxWidth - SidePad * 2 - gapTarget) / max(n, 1)).coerceIn(MinSlot, MaxSlot)
    val left = if (hasFab) (n + 1) / 2 else n
    val visual = if (rtl) items.asReversed() else items

    val slot by animateDpAsState(slotTarget, islandSpring(), label = "islandSlot")
    val barWidth by animateDpAsState(SidePad * 2 + slotTarget * n + gapTarget, islandSpring(), label = "islandWidth")
    val cx by animateDpAsState(SidePad + slotTarget * left + gapTarget / 2, islandSpring(), label = "islandNotchX")
    val fabT by animateFloatAsState(if (hasFab) 1f else 0f, islandSpring(), label = "islandFabT")

    fun slotX(v: Int): Dp = if (v < left) SidePad + slotTarget * v else SidePad + slotTarget * left + gapTarget + slotTarget * (v - left)

    val singleSelected = visual.count { it.selected } == 1
    val haloIndex = visual.indexOfFirst { it.selected }.coerceAtLeast(0)
    val pillWTarget = minOf(56.dp, slotTarget - 4.dp)
    val spotLeftTarget = (slotX(haloIndex) + (slotTarget - pillWTarget) / 2).value
    val spotRightTarget = spotLeftTarget + pillWTarget.value
    val spotId = remember { DockSpotMemory.newId() }
    val seed = remember { DockSpotMemory.seed() }
    val spotLeft = remember { Animatable(seed?.first ?: spotLeftTarget) }
    val spotRight = remember { Animatable(seed?.second ?: spotRightTarget) }
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    LaunchedEffect(spotLeftTarget, spotRightTarget, reduceMotion) {
        if (reduceMotion) {
            spotLeft.snapTo(spotLeftTarget)
            spotRight.snapTo(spotRightTarget)
            return@LaunchedEffect
        }
        val forward = spotLeftTarget >= spotLeft.value
        val lead = spring<Float>(dampingRatio = 0.8f, stiffness = 560f)
        val trail = spring<Float>(dampingRatio = 1f, stiffness = 150f)
        launch { spotLeft.animateTo(spotLeftTarget, if (forward) trail else lead) }
        launch { spotRight.animateTo(spotRightTarget, if (forward) lead else trail) }
    }
    SideEffect { if (singleSelected) DockSpotMemory.claim(spotId) { spotLeft.value to spotRight.value } }
    DisposableEffect(spotId) { onDispose { DockSpotMemory.leave(spotId) } }
    val superseded = DockSpotMemory.owner > spotId
    val haloAlpha by animateFloatAsState(if (singleSelected && !superseded) 1f else 0f, tween(140), label = "islandHaloAlpha")

    val notchDepth by animateDpAsState(if (hasFab) NotchDepth else 0.dp, islandSpring(), label = "islandNotchDepth")
    val shape = NotchedPillShape(cx.value, notchDepth.value)
    val fill = Brush.verticalGradient(
        if (colors.isDark) listOf(colors.containerHigh.copy(alpha = 0.82f), colors.container.copy(alpha = 0.78f))
        else listOf(colors.surface.copy(alpha = 0.97f), colors.container.copy(alpha = 0.97f))
    )
    val rim = Brush.verticalGradient(
        if (colors.isDark) listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.07f))
        else listOf(colors.surface, colors.primary.copy(alpha = 0.2f))
    )

    Box(modifier.width(barWidth).height(IslandBarHeight)) {
        Box(
            Modifier
                .matchParentSize()
                .shadow(10.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = if (colors.isDark) 0.30f else 0.10f), spotColor = Color.Black.copy(alpha = if (colors.isDark) 0.40f else 0.24f))
                .background(fill, shape)
                .border(1.dp, rim, shape)
        )
        if (haloAlpha > 0.01f) {
            val spotTop = ItemTop - 3.dp
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = haloAlpha }
                    .drawBehind {
                        val l = spotLeft.value.dp.toPx()
                        val r = spotRight.value.dp.toPx()
                        val w = (r - l).coerceAtLeast(1f)
                        val baseW = pillWTarget.toPx()
                        val stretch = ((w - baseW) / slotTarget.toPx()).coerceIn(0f, 1f)
                        val full = 28.dp.toPx()
                        val h = full * (1f - 0.14f * stretch)
                        val top = spotTop.toPx() + (full - h) / 2f
                        val radius = CornerRadius(minOf(16.dp.toPx(), h / 2f))
                        drawRoundRect(
                            Brush.verticalGradient(
                                listOf(colors.primary.copy(alpha = 0.20f), colors.primary.copy(alpha = 0.11f)),
                                top, top + h
                            ),
                            Offset(l, top), Size(w, h), radius
                        )
                        drawRoundRect(colors.primary.copy(alpha = 0.24f), Offset(l, top), Size(w, h), radius, style = Stroke(1.dp.toPx()))
                    }
            )
        }
        val initialIds = remember { visual.map { it.id }.toSet() }
        val ids = visual.map { it.id }
        val positions = visual.mapIndexed { v, it -> Triple(it, slotX(v), slotTarget) }
        val lastPositions = remember { arrayOfNulls<List<Triple<DockItem, Dp, Dp>>>(1) }
        val ghosts = remember(ids) { lastPositions[0].orEmpty().filter { it.first.id !in ids } }
        SideEffect { lastPositions[0] = positions }
        ghosts.forEach { (item, x, w) ->
            key("ghost-" + item.id) { IslandItemGhost(item, w, Modifier.offset { IntOffset(x.roundToPx(), 0) }) }
        }
        visual.forEachIndexed { v, item ->
            key(item.id) {
                val x by animateDpAsState(slotX(v), islandSpring(), label = "islandItemX")
                IslandItem(
                    item = item,
                    width = slot,
                    showOwnHalo = !singleSelected,
                    entrance = item.id !in initialIds,
                    modifier = Modifier.offset { IntOffset(x.roundToPx(), 0) }
                )
            }
        }
        val k = fabT.coerceIn(0f, 1f)
        val hold = fabHold.value
        if (hold != null && (hasFab || k > 0.05f)) {
            Box(
                Modifier
                    .offset { IntOffset((cx - HalfFab).roundToPx(), (-HalfFab - FabRaise).roundToPx()) }
                    .size(FrameFabSize)
                    .graphicsLayer {
                        val s = 0.25f + 0.75f * k
                        scaleX = s
                        scaleY = s
                        alpha = min(1f, k * 1.6f)
                    }
            ) { hold() }
            labelHold.value?.let { label ->
                Box(
                    Modifier
                        .offset { IntOffset((cx - FabGap / 2).roundToPx(), IslandLabelTop.roundToPx()) }
                        .width(FabGap)
                        .alpha(((k - 0.5f) * 2f).coerceIn(0f, 1f)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    FrameText(
                        label,
                        style = FrameTheme.typography.label.copy(fontSize = 11.sp, lineHeight = 14.sp),
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
