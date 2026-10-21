package com.frameout.app.viewer

import androidx.compose.foundation.Canvas
import com.frameout.app.billing.ProLockCorner
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.BrushScale
import com.frameout.core.edit.ShapeFill
import com.frameout.core.edit.ShapeKind
import com.frameout.core.edit.ShapeMark
import com.frameout.core.edit.ShapePainter
import com.frameout.core.util.EditStrings
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ShapeStrings
import com.frameout.app.billing.rememberProGate
import com.frameout.core.pro.ProFeature
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameControl
import kotlin.math.roundToInt

private const val DISABLED_ALPHA = 0.38f
private const val OPACITY_STEP = 5

@Composable
internal fun ShapeControls(draft: EditDraft, dims: ImageDims, tools: ViewerToolEditor) {
    val shapes = tools.shapes
    ShapePalette(draft.shapeKind, shapes::setKind)
    SwatchRow(draft.color, tools::setColor) { tools.setPickerOpen(true) }
    ShapeWidthRow(draft, dims, shapes)
    ShapeOpacityRow(draft, shapes)
    ShapeFillRow(draft, shapes)
    if (draft.shapeSelection != null) ShapeActions(shapes)
}

@Composable
private fun ShapePalette(selected: ShapeKind, onPick: (ShapeKind) -> Unit) {
    val gate = rememberProGate()
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), Arrangement.spacedBy(6.dp)) {
        ShapeKind.entries.forEach { kind -> ShapeTile(kind, kind == selected, gate.locked(false)) { gate.run(ProFeature.Shapes, kind.name) { onPick(kind) } } }
    }
}

@Composable
private fun ShapeTile(kind: ShapeKind, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint = if (selected) colors.primary else colors.onSurface
    val label = ShapeStrings.name(kind)
    Box(
        Modifier.size(52.dp).pressScale()
            .frameControl(FrameShapes.sm, selected = selected, glowAlpha = if (selected) 0.7f else 0f, cornerFraction = 0.3f)
            .clip(FrameShapes.sm)
            .frameSelectable(selected, Role.RadioButton, onClick)
            .semantics { contentDescription = label },
        Alignment.Center
    ) {
        Canvas(Modifier.size(30.dp)) {
            drawIntoCanvas { ShapePainter.paint(it.nativeCanvas, iconMark(kind, tint.toArgb()), size.width, size.height, null, size.width, true) }
        }
        ProLockCorner(locked)
    }
}

private fun iconMark(kind: ShapeKind, color: Int): ShapeMark {
    val width = 0.085f
    return when {
        kind.pointBased -> ShapeMark(kind, color, width, 0.14f, 0.84f, 0.86f, 0.18f)
        kind == ShapeKind.Callout -> ShapeMark(kind, color, width, 0.12f, 0.1f, 0.88f, 0.6f)
        kind == ShapeKind.Highlighter -> ShapeMark(kind, color, width, 0.1f, 0.3f, 0.9f, 0.7f)
        kind == ShapeKind.Magnifier -> ShapeMark(kind, color, width, 0.12f, 0.12f, 0.72f, 0.72f)
        kind.radial -> ShapeMark(kind, color, width, 0.2f, 0.2f, 0.8f, 0.8f)
        else -> ShapeMark(kind, color, width, 0.14f, 0.18f, 0.86f, 0.82f)
    }
}

@Composable
private fun ShapeWidthRow(draft: EditDraft, dims: ImageDims, shapes: ShapeEditor) {
    val lo = ShapeMark.MIN_WIDTH
    val hi = ShapeMark.MAX_WIDTH
    val fraction = BrushScale.toFraction(draft.shapeWidth, lo, hi)
    PrecisionRow(
        label = EditStrings.size(),
        valueText = "${BrushScale.pixels(draft.shapeWidth, dims.w, dims.h)} px",
        fraction = fraction,
        onFraction = { shapes.setWidth(BrushScale.fromFraction(it, lo, hi)) },
        onStep = { dir -> shapes.setWidth(BrushScale.stepPixels(draft.shapeWidth, dir, dims.w, dims.h, lo, hi)) }
    ) { BrushDot(Color(draft.color), fraction) }
}

@Composable
private fun ShapeOpacityRow(draft: EditDraft, shapes: ShapeEditor) {
    val lo = ShapeMark.MIN_OPACITY
    val value = draft.shapeOpacity
    PrecisionRow(
        label = EditStrings.opacity(),
        valueText = "${(value * 100f).roundToInt()}%",
        fraction = (value - lo) / (1f - lo),
        onFraction = { shapes.setOpacity(lo + it * (1f - lo)) },
        onStep = { dir -> shapes.setOpacity(((value * 100f).roundToInt() + dir * OPACITY_STEP) / 100f) }
    ) { RowIcon(FrameIconsExtra.Contrast) }
}

@Composable
private fun ShapeFillRow(draft: EditDraft, shapes: ShapeEditor) {
    val kind = draft.shapeKind
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
        Box(Modifier.weight(3f).alpha(if (kind.supportsFill) 1f else DISABLED_ALPHA)) {
            SegmentedChoice(
                listOf(
                    ChoiceOption(FrameIconsExtra.RectSelect, ShapeStrings.fillNone(), draft.shapeFill == ShapeFill.None) { shapes.setFill(ShapeFill.None) },
                    ChoiceOption(FrameIconsExtra.Contrast, ShapeStrings.fillSoft(), draft.shapeFill == ShapeFill.Soft) { shapes.setFill(ShapeFill.Soft) },
                    ChoiceOption(FrameIconsExtra.Solid, EditStrings.solid(), draft.shapeFill == ShapeFill.Solid) { shapes.setFill(ShapeFill.Solid) }
                )
            )
        }
        ChoiceChip(
            ShapeStrings.dashed(), draft.shapeDash && kind.supportsDash,
            Modifier.weight(1.2f).alpha(if (kind.supportsDash) 1f else DISABLED_ALPHA)
        ) { shapes.setDash(!draft.shapeDash) }
    }
}

@Composable
private fun ShapeActions(shapes: ShapeEditor) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        ChoiceChip(ShapeStrings.duplicate(), false, Modifier.weight(1f)) { shapes.duplicateSelected() }
        ChoiceChip(FrameStrings.delete(), false, Modifier.weight(1f)) { shapes.deleteSelected() }
    }
}
