package com.frameout.app.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.motion.rememberThemeWaveSource
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameColorScheme
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.darkColorScheme
import com.frameout.designsystem.theme.frameHairline
import com.frameout.designsystem.theme.lightColorScheme

@Composable
internal fun SettingsSwitch(checked: Boolean) {
    val colors = FrameTheme.colors
    val progress by animateFloatAsState(if (checked) 1f else 0f, frameSpring(), label = "switchProgress")
    Box(
        Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(FrameShapes.pill)
            .background(colors.containerHigh)
            .border(1.dp, frameHairline(), FrameShapes.pill)
    ) {
        Box(Modifier.fillMaxSize().alpha(progress).background(FrameGradients.Brand))
        Box(Modifier.fillMaxSize(), Alignment.CenterStart) {
            Box(
                Modifier
                    .padding(start = (3f + 20f * progress).dp)
                    .size(22.dp)
                    .clip(FrameShapes.pill)
                    .background(lerp(colors.onSurfaceVariant, Color.White, progress))
            )
        }
    }
}

internal class SegmentOption(val key: String, val label: String)

internal fun settingOptions(keys: List<String>): List<SegmentOption> = keys.map { SegmentOption(it, FrameStrings.settingValue(it)) }

@Composable
internal fun SettingsSegmented(options: List<SegmentOption>, value: String, onChange: (String) -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FrameShapes.pill)
            .background(colors.background)
            .border(1.dp, frameHairline(), FrameShapes.pill)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val active = option.key == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(FrameShapes.pill)
                    .then(if (active) Modifier.background(FrameGradients.Brand) else Modifier)
                    .frameSelectable(active, Role.RadioButton) { onChange(option.key) }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                Alignment.Center
            ) {
                FrameText(
                    option.label,
                    style = FrameTheme.typography.label,
                    color = if (active) Color.White else colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private val ThemeKeys = listOf("Dark", "AMOLED", "Light", "System")

@Composable
internal fun ThemePicker(value: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeKeys.forEach { key -> ThemeTile(key, key == value, Modifier.weight(1f)) { onChange(key) } }
    }
}

@Composable
private fun ThemeTile(key: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val wave = rememberThemeWaveSource()
    val tint = FrameTheme.accent.b
    val shape = FrameShapes.md
    val edge: Brush = if (selected) FrameGradients.Brand else SolidColor(frameHairline())
    Column(
        modifier.then(wave.modifier).clip(shape).frameSelectable(selected, Role.RadioButton) {
            if (!selected) wave.fire(tint)
            onClick()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(60.dp).clip(shape).border(if (selected) 2.dp else 1.dp, edge, shape)) {
            ThemeSwatch(key)
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .clip(FrameShapes.pill)
                        .background(FrameGradients.Brand),
                    Alignment.Center
                ) {
                    Image(painterResource(FrameIcons.Check), null, Modifier.size(12.dp), colorFilter = ColorFilter.tint(Color.White))
                }
            }
        }
        FrameText(
            FrameStrings.settingValue(key),
            style = FrameTheme.typography.label,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ThemeSwatch(key: String) {
    val accent = FrameTheme.accent
    val dark = remember(accent) { darkColorScheme(accent = accent) }
    val amoled = remember(accent) { darkColorScheme(amoled = true, accent = accent) }
    val light = remember(accent) { lightColorScheme(accent) }
    when (key) {
        "AMOLED" -> MiniScreen(amoled, Modifier.fillMaxSize())
        "Light" -> MiniScreen(light, Modifier.fillMaxSize())
        "System" -> Row(Modifier.fillMaxSize()) {
            MiniScreen(dark, Modifier.weight(1f).fillMaxHeight())
            MiniScreen(light, Modifier.weight(1f).fillMaxHeight())
        }
        else -> MiniScreen(dark, Modifier.fillMaxSize())
    }
}

@Composable
private fun MiniScreen(scheme: FrameColorScheme, modifier: Modifier) {
    Column(modifier.background(scheme.background).padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.width(22.dp).height(6.dp).background(scheme.primary, FrameShapes.pill))
        Box(Modifier.fillMaxWidth().height(8.dp).background(scheme.onSurface.copy(alpha = 0.16f), FrameShapes.pill))
        Box(Modifier.fillMaxWidth(0.7f).height(8.dp).background(scheme.onSurface.copy(alpha = 0.09f), FrameShapes.pill))
    }
}
