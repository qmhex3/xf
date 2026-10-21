package com.frameout.app.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.AdjustMatrix
import com.frameout.core.edit.BrushScale
import com.frameout.core.edit.CoverStyle
import com.frameout.core.edit.CropPreset
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.core.util.EditStrings
import com.frameout.core.util.ShapeStrings
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class ImageDims(val w: Int, val h: Int)

@Composable
internal fun RowIcon(icon: Int) {
    Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.onSurfaceVariant))
}

private fun signed(n: Int): String = if (n > 0) "+$n" else "$n"

@Composable
internal fun AdjustControls(draft: EditDraft, tools: ViewerToolEditor) {
    AdjustRow(FrameIconsExtra.Sun, EditStrings.brightness(), draft.brightness, AdjustMatrix.BRIGHTNESS_MIN, AdjustMatrix.BRIGHTNESS_MAX, 0f, tools::setBrightness)
    AdjustRow(FrameIconsExtra.Contrast, EditStrings.contrast(), draft.contrast, AdjustMatrix.CONTRAST_MIN, AdjustMatrix.CONTRAST_MAX, 1f, tools::setContrast)
    AdjustRow(FrameIconsExtra.Blur, EditStrings.saturation(), draft.saturation, AdjustMatrix.SATURATION_MIN, AdjustMatrix.SATURATION_MAX, 1f, tools::setSaturation)
}

@Composable
private fun AdjustRow(icon: Int, label: String, value: Float, lo: Float, hi: Float, neutral: Float, onChange: (Float) -> Unit) {
    PrecisionRow(
        label = label,
        valueText = signed(((value - neutral) * 100f).roundToInt()),
        fraction = (value - lo) / (hi - lo),
        onFraction = { onChange(lo + it * (hi - lo)) },
        onStep = { dir -> onChange((((value * 100f).roundToInt() + dir) / 100f).coerceIn(lo, hi)) }
    ) { RowIcon(icon) }
}

@Composable
internal fun DrawControls(draft: EditDraft, dims: ImageDims, tools: ViewerToolEditor) {
    val gate = com.frameout.app.billing.rememberProGate()
    SegmentedChoice(
        listOf(
            ChoiceOption(FrameIconsExtra.Brush, EditStrings.pen(), draft.drawMode == DrawMode.Pen) { tools.shapes.setMode(DrawMode.Pen) },
            ChoiceOption(FrameIconsExtra.Shapes, ShapeStrings.title(), draft.drawMode == DrawMode.Shapes) { gate.run(com.frameout.core.pro.ProFeature.Shapes, "mode") { tools.shapes.setMode(DrawMode.Shapes) } }
        )
    )
    if (draft.drawMode == DrawMode.Shapes) {
        ShapeControls(draft, dims, tools)
        return
    }
    SwatchRow(draft.color, tools::setColor) { tools.setPickerOpen(true) }
    val lo = BrushScale.DRAW_MIN
    val hi = BrushScale.DRAW_MAX
    val fraction = BrushScale.toFraction(draft.size, lo, hi)
    PrecisionRow(
        label = EditStrings.size(),
        valueText = "${BrushScale.pixels(draft.size, dims.w, dims.h)} px",
        fraction = fraction,
        onFraction = { tools.setSize(BrushScale.fromFraction(it, lo, hi)) },
        onStep = { dir -> tools.setSize(BrushScale.stepPixels(draft.size, dir, dims.w, dims.h, lo, hi)) }
    ) { BrushDot(Color(draft.color), fraction) }
}

@Composable
internal fun CoverControls(draft: EditDraft, dims: ImageDims, tools: ViewerToolEditor) {
    SegmentedChoice(
        listOf(
            ChoiceOption(FrameIconsExtra.Brush, EditStrings.pen(), draft.shape == CoverShape.Pen) { tools.setShape(CoverShape.Pen) },
            ChoiceOption(FrameIconsExtra.RectSelect, EditStrings.rectangle(), draft.shape == CoverShape.Rect) { tools.setShape(CoverShape.Rect) }
        )
    )
    SegmentedChoice(
        listOf(
            ChoiceOption(FrameIconsExtra.Blur, EditStrings.blur(), draft.style == CoverStyle.Blur) { tools.setStyle(CoverStyle.Blur) },
            ChoiceOption(FrameIconsExtra.Pixelate, EditStrings.pixelate(), draft.style == CoverStyle.Pixelate) { tools.setStyle(CoverStyle.Pixelate) },
            ChoiceOption(FrameIconsExtra.Solid, EditStrings.solid(), draft.style == CoverStyle.Solid) { tools.setStyle(CoverStyle.Solid) }
        )
    )
    StrengthRow(draft.strength, draft.style != CoverStyle.Solid, tools)
    CoverSizeRow(draft, dims, tools, draft.shape == CoverShape.Pen)
}

@Composable
private fun StrengthRow(strength: Float, enabled: Boolean, tools: ViewerToolEditor) {
    PrecisionRow(
        enabled = enabled,
        label = EditStrings.strength(),
        valueText = "${(strength * 100f).roundToInt()}%",
        fraction = strength,
        onFraction = tools::setStrength,
        onStep = { dir -> tools.setStrength((((strength * 100f).roundToInt() + dir) / 100f).coerceIn(0f, 1f)) }
    ) { RowIcon(FrameIconsExtra.Contrast) }
}

@Composable
private fun CoverSizeRow(draft: EditDraft, dims: ImageDims, tools: ViewerToolEditor, enabled: Boolean) {
    val lo = BrushScale.COVER_MIN
    val hi = BrushScale.COVER_MAX
    val fraction = BrushScale.toFraction(draft.coverSize, lo, hi)
    PrecisionRow(
        enabled = enabled,
        label = EditStrings.size(),
        valueText = "${BrushScale.pixels(draft.coverSize, dims.w, dims.h)} px",
        fraction = fraction,
        onFraction = { tools.setCoverSize(BrushScale.fromFraction(it, lo, hi)) },
        onStep = { dir -> tools.setCoverSize(BrushScale.stepPixels(draft.coverSize, dir, dims.w, dims.h, lo, hi)) }
    ) { BrushDot(FrameTheme.colors.onSurface, fraction) }
}

private const val RATIO_TOLERANCE = 0.02f

@Composable
internal fun CropControls(state: ViewerState, tools: ViewerToolEditor) {
    val shown = state.bitmap
    val rect = state.displayCrop()
    val widthPx = ((rect.r - rect.l) * (shown?.width ?: 0)).roundToInt()
    val heightPx = ((rect.b - rect.t) * (shown?.height ?: 0)).roundToInt()
    val ratio = if (heightPx > 0) widthPx.toFloat() / heightPx else 0f
    val matched = CropPreset.entries.firstOrNull { it != CropPreset.Free && abs(ratio - it.w.toFloat() / it.h) <= it.w.toFloat() / it.h * RATIO_TOLERANCE }
    val active = if (!state.hasCrop) CropPreset.Free else matched
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CropPreset.entries.forEach { preset ->
            ChoiceChip(
                label = if (preset == CropPreset.Free) FrameStrings.cropFree() else preset.label,
                selected = preset == active,
                modifier = Modifier.weight(1f)
            ) { tools.setCropRatio(preset) }
        }
    }
    if (shown != null) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FrameText("$widthPx × $heightPx", style = FrameTheme.typography.monoSmall, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun RotateControls(draft: EditDraft, tools: ViewerToolEditor) {
    val swapped = GeometryMath.swaps(draft.rotation)
    val mirroredAcross = if (swapped) draft.flipV else draft.flipH
    val mirroredDown = if (swapped) draft.flipH else draft.flipV
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionTile(FrameIconsExtra.Rotate, EditStrings.rotateLeft(), Modifier.weight(1f), mirror = true) { tools.rotate(-90) }
        ActionTile(FrameIconsExtra.Rotate, EditStrings.rotateRight(), Modifier.weight(1f)) { tools.rotate(90) }
        ActionTile(FrameIconsExtra.FlipH, FrameStrings.flipH(), Modifier.weight(1f), selected = mirroredAcross) { tools.flipHorizontal() }
        ActionTile(FrameIconsExtra.FlipV, FrameStrings.flipV(), Modifier.weight(1f), selected = mirroredDown) { tools.flipVertical() }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        FrameText("${draft.rotation}°", style = FrameTheme.typography.monoSmall, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}
