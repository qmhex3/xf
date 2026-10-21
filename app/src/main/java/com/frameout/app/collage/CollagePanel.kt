package com.frameout.app.collage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.frameout.app.viewer.ColorPickerPanel
import com.frameout.app.viewer.PanelIconButton
import com.frameout.app.viewer.ToolTab
import com.frameout.core.collage.COLLAGE_DEFAULT_WIDTH
import com.frameout.core.collage.COLLAGE_GAP_DEFAULT
import com.frameout.core.collage.CollageAspect
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.CollageStrings
import com.frameout.core.util.EditStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameSpinner
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private const val BODY_FRACTION = 0.26f
private val BODY_MIN = 168.dp
private val BODY_MAX = 208.dp
private val BODY_GAP = 12.dp

internal enum class CollageTab(val icon: Int, val label: () -> String) {
    Frames(FrameIcons.GridSize, CollageStrings::tabFrames),
    Style(FrameIconsExtra.Palette, CollageStrings::tabStyle),
    Canvas(FrameIconsExtra.Fit, CollageStrings::tabCanvas)
}

@Composable
fun CollagePanel(state: CollageState, onSave: () -> Unit, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(CollageTab.Frames) }
    GlassSurface(modifier.fillMaxWidth(), FrameShapes.lg) {
        Column {
            PanelHeader(tab, state, onSave)
            PanelTabs(tab) {
                state.pickerOpen = false
                tab = it
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(FrameTheme.colors.divider))
            PanelBody(tab, state)
        }
    }
}

@Composable
private fun PanelHeader(tab: CollageTab, state: CollageState, onSave: () -> Unit) {
    val canSave = state.ready && state.frames.isNotEmpty() && !state.saving
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FrameText(tab.label(), style = FrameTheme.typography.label, color = FrameTheme.colors.onSurface, maxLines = 1, modifier = Modifier.weight(1f))
        PanelIconButton(FrameIconsExtra.Undo, EditStrings.reset(), enabled = tab.isModified(state) && !state.saving) { tab.reset(state) }
        SaveButton(canSave, state.saving, onSave)
    }
}

@Composable
private fun SaveButton(enabled: Boolean, saving: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val description = CollageStrings.saveCollage()
    Row(
        Modifier
            .height(44.dp)
            .alpha(if (enabled || saving) 1f else 0.45f)
            .clip(FrameShapes.pill)
            .background(colors.primary)
            .frameClickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (saving) {
            FrameSpinner(size = 20.dp, color = colors.onPrimary)
        } else {
            Image(painterResource(FrameIcons.Save), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(colors.onPrimary))
        }
        val text = if (saving) FrameStrings.saving() else FrameStrings.save()
        FrameText(text, style = FrameTheme.typography.label.copy(fontWeight = FontWeight.SemiBold), color = colors.onPrimary, maxLines = 1)
    }
}

@Composable
private fun PanelTabs(current: CollageTab, onSelect: (CollageTab) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        CollageTab.entries.forEach { ToolTab(it.icon, it.label(), current == it, { onSelect(it) }, Modifier.weight(1f)) }
    }
}

@Composable
private fun bodyHeight(): Dp = (LocalConfiguration.current.screenHeightDp * BODY_FRACTION).dp.coerceIn(BODY_MIN, BODY_MAX)

@Composable
private fun PanelBody(tab: CollageTab, state: CollageState) {
    val height = bodyHeight()
    if (state.pickerOpen) {
        Box(Modifier.fillMaxWidth().height(height).verticalScroll(rememberScrollState()), Alignment.Center) {
            ColorPickerPanel(state.bgColor, { state.bgColor = it }) { state.pickerOpen = false }
        }
        return
    }
    key(tab) {
        Column(Modifier.fillMaxWidth().height(height).verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = height).padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(BODY_GAP, Alignment.CenterVertically)
            ) {
                when (tab) {
                    CollageTab.Frames -> FramesTab(state)
                    CollageTab.Style -> StyleTab(state)
                    CollageTab.Canvas -> CanvasTab(state)
                }
            }
        }
    }
}

private fun CollageTab.isModified(s: CollageState): Boolean = when (this) {
    CollageTab.Frames -> s.gap != COLLAGE_GAP_DEFAULT || s.rounded
    CollageTab.Style -> s.bgColor != AppPrefs.COLLAGE_BG_DEFAULT || s.stamps
    CollageTab.Canvas -> s.aspect != CollageAspect.Template || s.resolution != COLLAGE_DEFAULT_WIDTH
}

private fun CollageTab.reset(s: CollageState) {
    when (this) {
        CollageTab.Frames -> { s.gap = COLLAGE_GAP_DEFAULT; s.rounded = false }
        CollageTab.Style -> { s.bgColor = AppPrefs.COLLAGE_BG_DEFAULT; s.stamps = false }
        CollageTab.Canvas -> { s.aspect = CollageAspect.Template; s.resolution = COLLAGE_DEFAULT_WIDTH }
    }
}
