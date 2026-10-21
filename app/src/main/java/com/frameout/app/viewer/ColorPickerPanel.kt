package com.frameout.app.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.ColorMath
import com.frameout.core.util.EditStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun ColorPickerPanel(color: Int, onColor: (Int) -> Unit, onDone: () -> Unit) {
    var stored by remember { mutableStateOf(ColorMath.fromArgb(color)) }
    val hsv = if (ColorMath.toArgb(stored) == color) stored else ColorMath.fromArgb(color)
    LaunchedEffect(color) { if (ColorMath.toArgb(stored) != color) stored = ColorMath.fromArgb(color) }
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        ColorWheel(hsv, { next -> stored = next; onColor(ColorMath.toArgb(next)) }, Modifier.size(184.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FrameText(EditStrings.color(), style = FrameTheme.typography.label, color = FrameTheme.colors.onSurfaceVariant)
            ColorReadout(ColorMath.toArgb(hsv))
            DoneButton(onDone)
        }
    }
}

@Composable
private fun ColorReadout(argb: Int) {
    val colors = FrameTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(48.dp).clip(FrameShapes.md).background(Color(argb)).border(1.dp, colors.outline, FrameShapes.md))
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FrameText(ColorMath.hex(argb), style = FrameTheme.typography.mono, color = colors.onSurface)
        }
    }
}

@Composable
private fun DoneButton(onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(FrameShapes.pill).background(colors.primary).frameClickable(onClick = onClick).padding(vertical = 8.dp),
        Arrangement.Center,
        Alignment.CenterVertically
    ) {
        Image(painterResource(FrameIcons.Check), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(colors.onPrimary))
        Spacer(Modifier.width(6.dp))
        FrameText(FrameStrings.done(), style = FrameTheme.typography.label, color = colors.onPrimary)
    }
}
