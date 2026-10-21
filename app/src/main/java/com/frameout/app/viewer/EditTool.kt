package com.frameout.app.viewer

import androidx.compose.ui.unit.LayoutDirection
import com.frameout.designsystem.theme.rememberFrameAdaptive
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.AdjustMatrix
import com.frameout.core.edit.CoverStyle
import com.frameout.core.edit.DEFAULT_COVER_STRENGTH
import com.frameout.core.edit.EditMark
import com.frameout.core.edit.ShapeFill
import com.frameout.core.edit.ShapeKind
import com.frameout.core.edit.ShapeMark
import com.frameout.core.edit.FilterPresets
import com.frameout.core.edit.TextMark
import com.frameout.core.edit.Transform

enum class EditTool {
    None, Crop, Rotate, Adjust, Filter, Draw, Cover, Text;

    val ownsGestures: Boolean
        get() = this == Crop || this == Draw || this == Cover || this == Text

    val isColorTool: Boolean
        get() = this == Adjust || this == Filter

    val canvasSpace: Int
        get() = when (this) {
            None -> 0
            Crop -> 1
            else -> 2
        }
}

enum class ColorTarget { Brush, TextFill, TextOutline, TextBackground }

enum class TextTab { Style, Color, Effects }

enum class CoverShape { Pen, Rect }

enum class DrawMode { Pen, Shapes }

val BRUSH_COLORS = listOf(
    0xFFFF3B30.toInt(),
    0xFFFFCC00.toInt(),
    0xFF34C759.toInt(),
    0xFF0A84FF.toInt(),
    0xFFFFFFFF.toInt(),
    0xFF000000.toInt()
)

const val DEFAULT_DRAW_SIZE = 0.012f
const val DEFAULT_COVER_SIZE = 0.04f
internal val EDITOR_BODY_HEIGHT = 264.dp
internal val EDITOR_BODY_MIN_HEIGHT = 128.dp
internal val EDITOR_FIXED_HEIGHT = 232.dp
internal val EDITOR_TEXT_HEIGHT = 276.dp
private val PANEL_FALLBACK = 360.dp
internal val SIDE_PANEL_WIDTH = 380.dp
internal val BOTTOM_PANEL_MAX_WIDTH = 560.dp
private val PANEL_GAP = 4.dp

data class EditDraft(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val filter: String = FilterPresets.NONE,
    val filterStrength: Float = FilterPresets.DEFAULT_STRENGTH,
    val marks: List<EditMark> = emptyList(),
    val color: Int = BRUSH_COLORS.first(),
    val size: Float = DEFAULT_DRAW_SIZE,
    val style: CoverStyle = CoverStyle.Blur,
    val strength: Float = DEFAULT_COVER_STRENGTH,
    val shape: CoverShape = CoverShape.Pen,
    val coverSize: Float = DEFAULT_COVER_SIZE,
    val pickerOpen: Boolean = false,
    val pickerTarget: ColorTarget = ColorTarget.Brush,
    val selected: Int = -1,
    val text: TextMark = TextMark.Default,
    val rotation: Int = 0,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val drawMode: DrawMode = DrawMode.Pen,
    val shapeKind: ShapeKind = ShapeKind.TaperArrow,
    val shapeFill: ShapeFill = ShapeFill.None,
    val shapeDash: Boolean = false,
    val shapeOpacity: Float = 1f,
    val shapeWidth: Float = ShapeMark.DEFAULT_WIDTH,
    val selectedShape: Int = -1
) {
    val transformed: Boolean
        get() = rotation != 0 || flipH || flipV

    val adjusted: Boolean
        get() = brightness != 0f || contrast != 1f || saturation != 1f

    val filtered: Boolean
        get() = filter != FilterPresets.NONE

    val selectedText: TextMark?
        get() = marks.getOrNull(selected) as? TextMark

    val shapeSelection: ShapeMark?
        get() = marks.getOrNull(selectedShape) as? ShapeMark
}

@Composable
internal fun ViewerState.panelPadding(): PaddingValues {
    val inset = panelInset()
    return if (rememberFrameAdaptive().sideEditor) PaddingValues(end = inset) else PaddingValues(bottom = inset)
}

@Composable
internal fun ViewerState.panelInset(): Dp {
    val reduce = com.frameout.designsystem.a11y.LocalSystemA11y.current.reduceMotion
    val inset by androidx.compose.animation.core.animateDpAsState(
        targetValue = panelInsetTarget(),
        animationSpec = if (reduce) androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(0.9f, androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "panelInset"
    )
    return inset.coerceAtLeast(0.dp)
}

@Composable
private fun ViewerState.panelInsetTarget(): Dp {
    if (tool == EditTool.None) return 0.dp
    if (rememberFrameAdaptive().sideEditor) return sideInsetTarget()
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val keyboard = if (tool == EditTool.Text) WindowInsets.ime.asPaddingValues().calculateBottomPadding() else 0.dp
    val system = if (keyboard > navigation) keyboard else navigation
    if (panelPx <= 0) return PANEL_FALLBACK + system
    return with(LocalDensity.current) { panelPx.toDp() } + PANEL_GAP + system
}

@Composable
private fun ViewerState.sideInsetTarget(): Dp {
    val direction = LocalLayoutDirection.current
    val bars = WindowInsets.navigationBars.asPaddingValues()
    val system = if (direction == LayoutDirection.Rtl) bars.calculateLeftPadding(direction) else bars.calculateRightPadding(direction)
    if (panelPx <= 0) return SIDE_PANEL_WIDTH + system
    return with(LocalDensity.current) { panelPx.toDp() } + PANEL_GAP + system
}

internal fun ViewerState.transform() = Transform(scale, offsetX, offsetY)

internal fun ViewerState.previewFilter(): ColorFilter? {
    if (!tool.isColorTool) return null
    val neutral = !draft.adjusted && !draft.filtered
    if (neutral) return null
    return ColorFilter.colorMatrix(
        ColorMatrix(AdjustMatrix.values(draft.brightness, draft.contrast, draft.saturation, draft.filter, draft.filterStrength))
    )
}

@Composable
internal fun ViewerState.imagePadding(): PaddingValues {
    val side = rememberFrameAdaptive().sideEditor
    val reduce = com.frameout.designsystem.a11y.LocalSystemA11y.current.reduceMotion
    val direction = LocalLayoutDirection.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val spec = if (reduce) androidx.compose.animation.core.snap<Dp>() else androidx.compose.animation.core.spring<Dp>(0.9f, androidx.compose.animation.core.Spring.StiffnessMediumLow)
    val framed = controlsVisible && tool == EditTool.None
    val bottomZone = if (side) 12.dp else ViewerBarZone
    val top by androidx.compose.animation.core.animateDpAsState(if (framed) statusTop + 60.dp else 0.dp, spec, label = "imageTop")
    val bottom by androidx.compose.animation.core.animateDpAsState(if (framed) navBottom + bottomZone else 0.dp, spec, label = "imageBottom")
    val margin by androidx.compose.animation.core.animateDpAsState(if (framed) ViewerImageMargin else 0.dp, spec, label = "imageMargin")
    val rail by androidx.compose.animation.core.animateDpAsState(if (framed && side) 96.dp else 0.dp, spec, label = "imageRail")
    val panel = panelPadding()
    return PaddingValues(
        start = margin + panel.calculateStartPadding(direction),
        top = top,
        end = margin + rail + panel.calculateEndPadding(direction),
        bottom = bottom + panel.calculateBottomPadding()
    )
}

internal val ViewerImageMargin: Dp = 12.dp
internal val ViewerImageRadius: Dp = 16.dp
