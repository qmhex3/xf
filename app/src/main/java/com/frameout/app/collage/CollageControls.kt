package com.frameout.app.collage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.app.viewer.ChoiceChip
import com.frameout.app.viewer.PrecisionRow
import com.frameout.app.viewer.SwatchRow
import com.frameout.core.collage.COLLAGE_GAP_MAX
import com.frameout.core.collage.COLLAGE_RESOLUTIONS
import com.frameout.core.collage.CollageAspect
import com.frameout.core.collage.CollageRenderer
import com.frameout.core.util.CollageStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.roundToInt

@Composable
internal fun FramesTab(state: CollageState) {
    SpacingControl(state)
    OptionGroup(FrameIconsExtra.RectSelect, CollageStrings.corners()) {
        ChoiceRow(listOf(FrameStrings.square(), FrameStrings.rounded()), if (state.rounded) 1 else 0) { state.rounded = it == 1 }
    }
}

@Composable
internal fun StyleTab(state: CollageState) {
    OptionGroup(FrameIconsExtra.Palette, CollageStrings.background()) {
        SwatchRow(state.bgColor, { state.bgColor = it }) { state.pickerOpen = true }
    }
    OptionGroup(FrameIconsExtra.Timer, CollageStrings.time()) {
        ChoiceRow(listOf(CollageStrings.off(), CollageStrings.on()), if (state.stamps) 1 else 0) { state.stamps = it == 1 }
    }
}

@Composable
internal fun CanvasTab(state: CollageState) {
    val aspects = CollageAspect.entries
    val size = remember(state.resolution, state.aspect, state.template) {
        CollageRenderer.outputSize(state.resolution, state.canvasTemplate.aspect)
    }
    OptionGroup(FrameIcons.Crop, CollageStrings.aspect()) {
        ChoiceRow(aspects.map { CollageStrings.aspectName(it.key) }, aspects.indexOf(state.aspect)) { state.aspect = aspects[it] }
    }
    OptionGroup(FrameIconsExtra.ImageFormat, CollageStrings.resolutionSize(size.first, size.second)) {
        ChoiceRow(
            COLLAGE_RESOLUTIONS.map { CollageStrings.resolutionName(it) },
            COLLAGE_RESOLUTIONS.indexOf(state.resolution).coerceAtLeast(0)
        ) { state.resolution = COLLAGE_RESOLUTIONS[it] }
    }
}

@Composable
private fun SpacingControl(state: CollageState) {
    val max = COLLAGE_GAP_MAX
    PrecisionRow(
        label = CollageStrings.spacing(),
        valueText = "${state.gap} px",
        fraction = state.gap / max.toFloat(),
        onFraction = { state.gap = (it * max).roundToInt().coerceIn(0, max) },
        onStep = { state.gap = (state.gap + it).coerceIn(0, max) }
    ) {
        GroupIcon(FrameIcons.GridSize)
    }
}

@Composable
private fun GroupIcon(icon: Int) {
    Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.onSurfaceVariant))
}

@Composable
internal fun OptionGroup(icon: Int, label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp), Alignment.Center) { GroupIcon(icon) }
            Spacer(Modifier.width(8.dp))
            FrameText(label, style = FrameTheme.typography.label, color = FrameTheme.colors.onSurface)
        }
        content()
    }
}

@Composable
internal fun ChoiceRow(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, text -> ChoiceChip(text, i == selectedIndex, Modifier.weight(1f)) { onSelect(i) } }
    }
}
