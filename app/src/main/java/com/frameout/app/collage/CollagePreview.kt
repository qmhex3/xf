package com.frameout.app.collage

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.collage.CellCrop
import com.frameout.core.collage.CollageTemplate
import com.frameout.core.collage.CropGeometry
import com.frameout.core.collage.collageUnit
import com.frameout.core.collage.toPx
import com.frameout.core.util.CollageStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import kotlin.math.roundToInt

class PreviewSlot(val bitmap: Bitmap?, val stamp: String?, val number: Int, val crop: CellCrop = CellCrop())

class PreviewStyle(val gapRef: Int, val cornerRef: Float, val background: Color, val showStamps: Boolean)

@Composable
fun CollagePreview(
    template: CollageTemplate,
    slots: List<PreviewSlot?>,
    style: PreviewStyle,
    selectedCell: Int?,
    onCellClick: (Int) -> Unit,
    onCropChange: (Int, CellCrop) -> Unit,
    onSwap: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            val maxW = constraints.maxWidth.toFloat()
            val maxH = constraints.maxHeight.toFloat()
            val width = if (maxW / maxH > template.aspect) maxH * template.aspect else maxW
            PreviewCanvas(template, slots, style, selectedCell, onCellClick, onCropChange, onSwap, width, width / template.aspect)
        }
    }
}

@Composable
private fun PreviewCanvas(
    template: CollageTemplate,
    slots: List<PreviewSlot?>,
    style: PreviewStyle,
    selectedCell: Int?,
    onCellClick: (Int) -> Unit,
    onCropChange: (Int, CellCrop) -> Unit,
    onSwap: (Int, Int) -> Unit,
    width: Float,
    height: Float
) {
    val density = LocalDensity.current
    val colors = FrameTheme.colors
    val bg by animateColorAsState(style.background, label = "collageBg")
    val unit = collageUnit(width)
    val frame = RoundedCornerShape(6.dp)
    val rects = template.cells.map { it.toPx(width, height, style.gapRef * unit) }
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    val target = dragFrom?.let { from -> rects.indexOfFirst { dragPos.x in it.left..it.right && dragPos.y in it.top..it.bottom }.takeIf { it >= 0 && it != from && slots.getOrNull(it) != null } }
    Box(
        Modifier
            .size(with(density) { width.toDp() }, with(density) { height.toDp() })
            .frameGlow(colors.primary, { if (colors.isDark) 0.10f else 0f }, 22.dp, { 0.04f })
            .clip(frame)
            .background(bg)
            .border(1.dp, frameHairline(), frame)
    ) {
        template.cells.indices.forEach { i ->
            val r = rects[i]
            CollageCell(
                slot = slots.getOrNull(i),
                number = i + 1,
                radiusPx = style.cornerRef * unit,
                unit = unit,
                cellWidth = r.width,
                cellHeight = r.height,
                showStamp = style.showStamps,
                selected = selectedCell == i,
                dragging = dragFrom == i,
                dragOffset = if (dragFrom == i) dragPos - dragStart else Offset.Zero,
                dropTarget = target == i,
                onDragStart = { local -> dragFrom = i; dragStart = Offset(r.left, r.top) + local; dragPos = dragStart },
                onDragBy = { dragPos += it },
                onDragEnd = { val to = target; val from = dragFrom; dragFrom = null; if (from != null && to != null) onSwap(from, to) },
                onDragCancel = { dragFrom = null },
                modifier = Modifier
                    .absoluteOffset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
                    .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() }),
                onClick = { onCellClick(i) },
                onCrop = { if (dragFrom == null) onCropChange(i, it) }
            )
        }
    }
}

@Composable
private fun CollageCell(
    slot: PreviewSlot?,
    number: Int,
    radiusPx: Float,
    unit: Float,
    cellWidth: Float,
    cellHeight: Float,
    showStamp: Boolean,
    selected: Boolean,
    dragging: Boolean,
    dragOffset: Offset,
    dropTarget: Boolean,
    onDragStart: (Offset) -> Unit,
    onDragBy: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier,
    onClick: () -> Unit,
    onCrop: (CellCrop) -> Unit
) {
    val dragStartNow by rememberUpdatedState(onDragStart)
    val dragByNow by rememberUpdatedState(onDragBy)
    val dragEndNow by rememberUpdatedState(onDragEnd)
    val dragCancelNow by rememberUpdatedState(onDragCancel)
    val dragHaptics = rememberFrameHaptics()
    val colors = FrameTheme.colors
    val shape = RoundedCornerShape(with(LocalDensity.current) { radiusPx.toDp() })
    val haptics = rememberFrameHaptics()
    val desc = if (slot != null) CollageStrings.frameN(number) else CollageStrings.emptySlot()
    Box(
        modifier
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                val k = if (dragging) 1.06f else 1f
                scaleX = k
                scaleY = k
                alpha = if (dragging) 0.85f else 1f
            }
            .clip(shape)
            .background(colors.container)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                haptics.perform(FrameHaptic.Select)
                onClick()
            }
            .cropGestures(slot?.bitmap, cellWidth, cellHeight, slot?.crop ?: CellCrop(), onCrop)
            .then(
                if (slot == null) Modifier else Modifier.pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragHaptics.perform(FrameHaptic.LongPress); dragStartNow(it) },
                        onDrag = { change, amount -> change.consume(); dragByNow(amount) },
                        onDragEnd = { dragEndNow() },
                        onDragCancel = { dragCancelNow() }
                    )
                }
            )
            .semantics { contentDescription = desc; this.selected = selected }
    ) {
        if (slot == null) EmptySlot() else FilledSlot(slot, number, showStamp, unit, cellWidth, cellHeight)
        if (dropTarget) {
            Box(Modifier.fillMaxSize().background(colors.primary.copy(alpha = 0.28f)).border(3.dp, colors.primary, shape))
        }
        if (selected) {
            Box(Modifier.fillMaxSize().background(colors.primary.copy(alpha = 0.18f)).border(2.5.dp, colors.primary, shape))
        }
    }
}

@Composable
private fun BoxScope.FilledSlot(slot: PreviewSlot, number: Int, showStamp: Boolean, unit: Float, cellWidth: Float, cellHeight: Float) {
    val image = remember(slot.bitmap) { slot.bitmap?.asImageBitmap() }
    val bitmap = slot.bitmap
    if (image != null && bitmap != null) {
        val crop = CropGeometry(bitmap.width.toFloat(), bitmap.height.toFloat(), cellWidth, cellHeight).clamp(slot.crop)
        val shift = Modifier.graphicsLayer {
            scaleX = crop.scale
            scaleY = crop.scale
            translationX = crop.offsetX * cellWidth
            translationY = crop.offsetY * cellHeight
        }
        Image(image, null, Modifier.fillMaxSize().then(shift), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.fillMaxSize().background(FrameTheme.colors.containerHigh))
    }
    if (showStamp && !slot.stamp.isNullOrBlank()) StampPill(slot.stamp, unit, cellWidth)
    NumberBadge(number)
}

@Composable
private fun EmptySlot() {
    val tint = FrameTheme.colors.onSurfaceVariant
    Canvas(Modifier.fillMaxSize()) {
        val inset = 6.dp.toPx()
        val dashed = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        drawRoundRect(
            tint.copy(alpha = 0.28f), Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2),
            CornerRadius(8.dp.toPx()), style = Stroke(1.2.dp.toPx(), pathEffect = dashed)
        )
        val c = Offset(size.width / 2f, size.height / 2f)
        val arm = minOf(size.width, size.height) * 0.1f
        val w = 1.6.dp.toPx()
        drawLine(tint.copy(alpha = 0.4f), Offset(c.x - arm, c.y), Offset(c.x + arm, c.y), w, StrokeCap.Round)
        drawLine(tint.copy(alpha = 0.4f), Offset(c.x, c.y - arm), Offset(c.x, c.y + arm), w, StrokeCap.Round)
    }
}

@Composable
private fun NumberBadge(n: Int) {
    Box(Modifier.padding(6.dp).size(20.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape), contentAlignment = Alignment.Center) {
        FrameText("$n", style = FrameTheme.typography.label.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}

@Composable
private fun BoxScope.StampPill(text: String, unit: Float, cellWidth: Float) {
    val density = LocalDensity.current
    val fontPx = (cellWidth * 0.07f).coerceIn(22f * unit, 44f * unit)
    Box(
        Modifier
            .align(Alignment.BottomStart)
            .padding(with(density) { (fontPx * 0.6f).toDp() })
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = with(density) { (fontPx * 0.55f).toDp() }, vertical = with(density) { (fontPx * 0.32f).toDp() })
    ) {
        val font = FrameTheme.typography.label.copy(fontSize = with(density) { fontPx.toSp() }, fontWeight = FontWeight.SemiBold, lineHeight = with(density) { (fontPx * 1.1f).toSp() })
        FrameText(text, style = font, color = Color.White, maxLines = 1)
    }
}
