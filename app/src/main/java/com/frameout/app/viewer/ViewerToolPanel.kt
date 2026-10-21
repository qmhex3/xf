package com.frameout.app.viewer

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.frameout.designsystem.theme.rememberFrameAdaptive
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.core.util.EditStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private class EditorTab(val tool: EditTool, val icon: Int, val label: () -> String, val title: () -> String)

private val EDITOR_TABS = listOf(
    EditorTab(EditTool.Crop, FrameIcons.Crop, FrameStrings::crop, EditStrings::cropTitle),
    EditorTab(EditTool.Rotate, FrameIconsExtra.Rotate, FrameStrings::rotate, EditStrings::rotateTitle),
    EditorTab(EditTool.Adjust, FrameIconsExtra.Adjust, EditStrings::adjust, EditStrings::adjust),
    EditorTab(EditTool.Filter, FrameIconsExtra.PhotoFilter, EditStrings::filters, EditStrings::filters),
    EditorTab(EditTool.Draw, FrameIconsExtra.Brush, EditStrings::draw, EditStrings::draw),
    EditorTab(EditTool.Cover, FrameIconsExtra.Cover, EditStrings::cover, EditStrings::cover),
    EditorTab(EditTool.Text, FrameIconsExtra.Text, EditStrings::text, EditStrings::text)
)

@Composable
internal fun ViewerToolPanel(state: ViewerState, tools: ViewerToolEditor) {
    val adaptive = rememberFrameAdaptive()
    val side = adaptive.sideEditor
    val bodyMax = if (side) (adaptive.height - 200.dp).coerceAtLeast(EDITOR_BODY_MIN_HEIGHT) else (adaptive.height * 0.42f).coerceIn(EDITOR_BODY_MIN_HEIGHT, EDITOR_BODY_HEIGHT)
    val textMax = if (side) bodyMax else (adaptive.height * 0.34f).coerceIn(EDITOR_BODY_MIN_HEIGHT, EDITOR_TEXT_HEIGHT)
    val keyboard = if (state.tool == EditTool.Text) Modifier.imePadding() else Modifier
    val enter = rememberPanelEnter()
    val frame = if (side) Modifier.statusBarsPadding() else Modifier
    Box(Modifier.fillMaxSize().navigationBarsPadding().then(frame).then(keyboard), contentAlignment = if (side) Alignment.CenterEnd else Alignment.BottomCenter) {
        GlassSurface(
            shape = FrameShapes.lg,
            glow = FrameTheme.colors.primary,
            modifier = (if (side) Modifier.fillMaxHeight().width(SIDE_PANEL_WIDTH) else Modifier.widthIn(max = BOTTOM_PANEL_MAX_WIDTH).fillMaxWidth())
                .onSizeChanged { tools.setPanelHeight(if (side) it.width else it.height) }
                .graphicsLayer {
                    alpha = enter.value * (if (state.toolBusy) 0.55f else 1f)
                    val shift = (1f - enter.value) * 48.dp.toPx()
                    if (side) translationX = shift else translationY = shift
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(Modifier.animateContentSize(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow))) {
                PanelHeader(state, tools)
                PanelTabs(state.tool, tools)
                Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(FrameTheme.colors.divider))
                if (state.draft.pickerOpen) {
                    Box(Modifier.fillMaxWidth().height(minOf(EDITOR_FIXED_HEIGHT, bodyMax)), Alignment.Center) {
                        ColorPickerPanel(pickerColorFor(state.draft), tools::setPickerColor) { tools.setPickerOpen(false) }
                    }
                } else {
                    AnimatedToolBody(state) { PanelBody(it, tools, bodyMax, textMax) }
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(state: ViewerState, tools: ViewerToolEditor) {
    val draft = state.draft
    val reset: Reset = when (state.tool) {
        EditTool.Crop -> Reset(state.hasCrop, EditStrings.resetCrop(), tools::resetCrop)
        EditTool.Rotate -> Reset(draft.transformed, EditStrings.resetRotate(), tools::resetRotate)
        EditTool.Adjust -> Reset(draft.adjusted, EditStrings.resetAdjust(), tools::resetAdjust)
        EditTool.Filter -> Reset(draft.filtered, EditStrings.resetFilter(), tools::resetFilter)
        else -> Reset(draft.marks.isNotEmpty(), EditStrings.undoMark(), tools::undoMark)
    }
    val title = EDITOR_TABS.firstOrNull { it.tool == state.tool }?.title?.invoke().orEmpty()
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PanelIconButton(FrameIcons.Close, FrameStrings.cancel(), onClick = tools::requestCancel)
        FrameText(
            title,
            style = FrameTheme.typography.label,
            color = FrameTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        PanelIconButton(FrameIconsExtra.Undo, reset.label, reset.enabled, reset.action)
        PanelDoneButton(FrameStrings.done(), FrameIcons.Check, tools::commit)
    }
}

private class Reset(val enabled: Boolean, val label: String, val action: () -> Unit)

@Composable
private fun PanelTabs(current: EditTool, tools: ViewerToolEditor) {
    val gate = com.frameout.app.billing.rememberProGate()
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        EDITOR_TABS.forEach { tab ->
            ToolTab(tab.icon, tab.label(), current == tab.tool, { selectTab(gate, tab.tool, tools) }, Modifier.weight(1f))
        }
    }
}

private fun selectTab(gate: com.frameout.app.billing.ProGate, tool: EditTool, tools: ViewerToolEditor) {
    if (tool == EditTool.Text || tool == EditTool.Cover) gate.run(com.frameout.core.pro.ProFeature.EditTools, tool.name) { tools.select(tool) } else tools.select(tool)
}

@Composable
private fun PanelBody(state: ViewerState, tools: ViewerToolEditor, bodyMax: Dp, textMax: Dp) {
    val dims = ImageDims(state.bitmap?.width ?: 1000, state.bitmap?.height ?: 1000)
    val tool = state.tool
    if (tool == EditTool.Text) {
        Column(
            Modifier.fillMaxWidth().height(textMax).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(BODY_GAP)
        ) { TextControls(state.displayDraft(), dims, tools) }
        return
    }
    key(tool) {
        Column(Modifier.fillMaxWidth().heightIn(max = bodyMax).verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = EDITOR_BODY_MIN_HEIGHT).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(BODY_GAP, Alignment.CenterVertically)
            ) {
                when (tool) {
                    EditTool.Crop -> CropControls(state, tools)
                    EditTool.Rotate -> RotateControls(state.draft, tools)
                    EditTool.Adjust -> AdjustControls(state.draft, tools)
                    EditTool.Filter -> FilterControls(state, tools)
                    EditTool.Draw -> DrawControls(state.draft, dims, tools)
                    EditTool.Cover -> CoverControls(state.draft, dims, tools)
                    EditTool.Text, EditTool.None -> Unit
                }
            }
        }
    }
}

@Composable
private fun rememberPanelEnter(): androidx.compose.runtime.State<Float> {
    var shown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { shown = true }
    val reduce = com.frameout.designsystem.a11y.LocalSystemA11y.current.reduceMotion
    return androidx.compose.animation.core.animateFloatAsState(
        if (shown) 1f else 0f,
        if (reduce) androidx.compose.animation.core.snap() else spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "panelEnter"
    )
}

private val BODY_GAP = 12.dp
