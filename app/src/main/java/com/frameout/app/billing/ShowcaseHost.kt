package com.frameout.app.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.frameout.core.pro.ShowcaseKind
import com.frameout.designsystem.theme.FrameAccent
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.FrameThemeMode

@Composable
fun PaywallShowcase(kind: ShowcaseKind, selected: FrameAccent, onPick: (FrameAccent) -> Unit, modifier: Modifier = Modifier) {
    ShowcaseStage(modifier, decorative = kind != ShowcaseKind.Accent) {
        if (kind == ShowcaseKind.Accent) AccentShowcase(selected, onPick) else ArtDemo(kind)
    }
}

@Composable
private fun ArtDemo(kind: ShowcaseKind) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        when (kind) {
            ShowcaseKind.Filter -> FilterShowcase()
            ShowcaseKind.Shapes -> ShapesShowcase()
            ShowcaseKind.Format -> FormatShowcase()
            ShowcaseKind.Smart -> SmartShowcase()
            ShowcaseKind.Export -> ExportShowcase()
            ShowcaseKind.Tool -> ToolShowcase()
            ShowcaseKind.Accent -> Unit
        }
    }
}

@Composable
fun RetintedTheme(picked: FrameAccent?, content: @Composable () -> Unit) {
    val colors = FrameTheme.colors
    val mode = when {
        !colors.isDark -> FrameThemeMode.Light
        colors.background == Color.Black -> FrameThemeMode.Amoled
        else -> FrameThemeMode.Dark
    }
    FrameTheme(mode = mode, glassIntensity = FrameTheme.glassIntensity, accent = picked ?: FrameTheme.accent, content = content)
}
