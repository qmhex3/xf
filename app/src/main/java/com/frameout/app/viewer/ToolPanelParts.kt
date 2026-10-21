package com.frameout.app.viewer

import com.frameout.designsystem.theme.frameHairline
import com.frameout.designsystem.theme.frameControl
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameHoldRepeat
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameSlider
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill

internal val HUE_COLORS = listOf(
    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000)
)

internal class ChoiceOption(val icon: Int, val label: String, val selected: Boolean, val onClick: () -> Unit)

@Composable
internal fun PanelIconButton(icon: Int, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).pressScale().alpha(if (enabled) 1f else 0.35f).clip(CircleShape).frameControl(CircleShape).frameClickable(enabled = enabled, onClick = onClick),
        Alignment.Center
    ) {
        Image(painterResource(icon), label, Modifier.size(20.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.onSurface))
    }
}

@Composable
internal fun PanelDoneButton(label: String, icon: Int, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(Modifier.size(40.dp).pressScale().frameControl(CircleShape, selected = true, solid = true, glowAlpha = 0.55f, glowSpread = 6.dp).clip(CircleShape).frameClickable(onClick = onClick), Alignment.Center) {
        Image(painterResource(icon), label, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color.White))
    }
}

@Composable
internal fun ToolTab(icon: Int, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, tween(200), label = "tabTint")
    Column(
        modifier.pressScale().heightIn(min = TAB_HEIGHT)
            .then(if (selected) Modifier.frameControl(FrameShapes.sm, selected = true, glowAlpha = 0.8f, cornerFraction = 0.3f) else Modifier)
            .clip(FrameShapes.sm)
            .frameSelectable(selected, Role.Tab, onClick).padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(tint))
        FrameText(
            label,
            style = FrameTheme.typography.label.copy(fontSize = 11.sp),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun ActionTile(icon: Int, label: String, modifier: Modifier = Modifier, selected: Boolean = false, mirror: Boolean = false, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurface, tween(200), label = "tileTint")
    Column(
        modifier.pressScale().heightIn(min = 76.dp)
            .frameControl(FrameShapes.md, selected = selected, glowAlpha = if (selected) 0.8f else 0f, cornerFraction = 0.25f)
            .clip(FrameShapes.md)
            .frameClickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
    ) {
        Image(
            painterResource(icon), null,
            Modifier.size(26.dp).graphicsLayer { if (mirror) scaleX = -1f },
            colorFilter = ColorFilter.tint(tint)
        )
        FrameText(label, style = FrameTheme.typography.label, color = tint, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, tween(200), label = "chipTint")
    Box(
        modifier.pressScale().heightIn(min = 40.dp)
            .frameControl(FrameShapes.sm, selected = selected, glowAlpha = if (selected) 0.7f else 0f, cornerFraction = 0.3f)
            .clip(FrameShapes.sm)
            .frameSelectable(selected, Role.RadioButton, onClick),
        Alignment.Center
    ) { FrameText(label, style = FrameTheme.typography.label, color = tint, maxLines = 1) }
}

internal val TAB_HEIGHT = 52.dp

@Composable
internal fun SegmentedChoice(options: List<ChoiceOption>, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    Row(
        modifier.fillMaxWidth().clip(FrameShapes.md).background(colors.fieldFill).border(1.dp, frameHairline(), FrameShapes.md).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val tint = if (option.selected) colors.primary else colors.onSurfaceVariant
            Row(
                Modifier.weight(1f)
                    .then(if (option.selected) Modifier.frameControl(FrameShapes.sm, selected = true, glowAlpha = 0.7f, cornerFraction = 0.3f) else Modifier)
                    .clip(FrameShapes.sm)
                    .frameSelectable(option.selected, Role.RadioButton, option.onClick).padding(vertical = 7.dp),
                Arrangement.Center,
                Alignment.CenterVertically
            ) {
                Image(painterResource(option.icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(tint))
                Spacer(Modifier.width(6.dp))
                FrameText(option.label, style = FrameTheme.typography.label, color = tint)
            }
        }
    }
}

@Composable
internal fun PrecisionRow(
    label: String,
    valueText: String,
    fraction: Float,
    onFraction: (Float) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: @Composable () -> Unit
) {
    Box(modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.38f)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp), Alignment.Center) { leading() }
                Spacer(Modifier.width(8.dp))
                FrameText(label, style = FrameTheme.typography.label, color = FrameTheme.colors.onSurface, modifier = Modifier.weight(1f))
                ValueChip(valueText)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StepButton(FrameIconsExtra.Minus, EditStrings.decrease()) { onStep(-1) }
                FrameSlider(fraction, onFraction, 0f, 1f, label, Modifier.weight(1f).height(28.dp))
                StepButton(FrameIconsExtra.Plus, EditStrings.increase()) { onStep(1) }
            }
        }
        if (!enabled) Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
    }
}

@Composable
private fun StepButton(icon: Int, label: String, onStep: () -> Unit) {
    Box(Modifier.size(34.dp).frameHoldRepeat(onStep).semantics { contentDescription = label }, Alignment.Center) {
        Image(painterResource(icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.primary))
    }
}

@Composable
private fun ValueChip(text: String) {
    Box(Modifier.width(60.dp).clip(FrameShapes.xs).background(FrameTheme.colors.primary.copy(alpha = 0.12f)).border(1.dp, FrameTheme.colors.primary.copy(alpha = 0.28f), FrameShapes.xs).padding(vertical = 4.dp), Alignment.Center) {
        FrameText(text, style = FrameTheme.typography.monoSmall, color = FrameTheme.colors.primary)
    }
}

@Composable
internal fun SwatchRow(color: Int, onPick: (Int) -> Unit, onCustom: () -> Unit) {
    val custom = BRUSH_COLORS.none { it == color }
    val names = BrushColorNames.all()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BRUSH_COLORS.forEachIndexed { i, preset -> Swatch(preset, names[i], color == preset, Modifier.weight(1f)) { onPick(preset) } }
        if (custom) Swatch(color, EditStrings.customColor(), true, Modifier.weight(1f)) { onCustom() }
        RainbowButton(Modifier.weight(1f), onCustom)
    }
}

@Composable
private fun Swatch(color: Int, name: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ring = if (selected) FrameTheme.colors.primary else FrameTheme.colors.outline
    Box(modifier.frameSelectable(selected, Role.RadioButton, onClick).semantics { contentDescription = name }, Alignment.Center) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Color(color)).border(if (selected) 3.dp else 1.dp, ring, CircleShape))
    }
}

@Composable
private fun RainbowButton(modifier: Modifier, onClick: () -> Unit) {
    val rainbow = Brush.sweepGradient(HUE_COLORS)
    Box(modifier.frameClickable(onClick = onClick).semantics { contentDescription = EditStrings.customColor() }, Alignment.Center) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(FrameTheme.colors.fieldFill).border(3.dp, rainbow, CircleShape), Alignment.Center) {
            Image(painterResource(FrameIconsExtra.Palette), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.onSurface))
        }
    }
}

@Composable
internal fun BrushDot(color: Color, fraction: Float) {
    val diameter = (5f + 17f * fraction).dp
    Box(Modifier.size(diameter).clip(CircleShape).background(color).border(1.dp, FrameTheme.colors.outline, CircleShape))
}

internal object BrushColorNames {
    fun all(): List<String> = listOf(
        EditStrings.red(), EditStrings.yellow(), EditStrings.green(), EditStrings.blue(), EditStrings.white(), EditStrings.black()
    )
}
