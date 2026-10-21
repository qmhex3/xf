package com.frameout.app.gallery

import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.a11y.frameClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.components.FrameSectionTitle
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.FrameThemeMode
import com.frameout.designsystem.theme.GlassIntensity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeControls(
    themeMode: FrameThemeMode,
    glass: GlassIntensity,
    onTheme: (FrameThemeMode) -> Unit,
    onGlass: (GlassIntensity) -> Unit
) {
    FrameSectionTitle("Theme")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeChip("Dark", themeMode == FrameThemeMode.Dark) { onTheme(FrameThemeMode.Dark) }
        ThemeChip("Light", themeMode == FrameThemeMode.Light) { onTheme(FrameThemeMode.Light) }
        ThemeChip("AMOLED", themeMode == FrameThemeMode.Amoled) { onTheme(FrameThemeMode.Amoled) }
    }
    FrameSectionTitle("Glass intensity")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeChip("Full", glass == GlassIntensity.Full) { onGlass(GlassIntensity.Full) }
        ThemeChip("Light", glass == GlassIntensity.Light) { onGlass(GlassIntensity.Light) }
        ThemeChip("Off", glass == GlassIntensity.Off) { onGlass(GlassIntensity.Off) }
    }
}

@Composable
fun GlassDemo() {
    FrameSectionTitle("Glass surfaces")
    GlassSurface(
        shape = FrameShapes.md,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            FrameText("Glass card", color = FrameTheme.colors.onSurface)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconsDemo() {
    FrameSectionTitle("Icons")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            FrameIcons.Library, FrameIcons.Settings, FrameIcons.VideoAdd,
            FrameIcons.Play, FrameIcons.Pause, FrameIcons.Check, FrameIcons.Close,
            FrameIcons.Back, FrameIcons.Trash, FrameIcons.Save, FrameIcons.Share,
            FrameIcons.Crop, FrameIcons.Collage, FrameIcons.Star
        ).forEach { IconPreview(it) }
    }
}

@Composable
fun TypeDemo() {
    FrameSectionTitle("Typography")
    FrameText("Display", style = FrameTheme.typography.display)
    FrameText("Title", style = FrameTheme.typography.title)
    FrameText("Body text for content", style = FrameTheme.typography.body)
    FrameText("00:12:34.567", style = FrameTheme.typography.mono)
}

@Composable
fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val bg = if (selected) colors.primary else colors.container
    val fg = if (selected) colors.onPrimary else colors.onSurface
    Box(
        modifier = Modifier
            .background(bg, FrameShapes.pill)
            .frameSelectable(selected, androidx.compose.ui.semantics.Role.RadioButton, onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        FrameText(label, style = FrameTheme.typography.label, color = fg)
    }
}

@Composable
fun IconPreview(icon: Int) {
    Image(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(28.dp),
        colorFilter = ColorFilter.tint(FrameTheme.colors.onSurface)
    )
}
