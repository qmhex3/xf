package com.frameout.app.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.dock.FrameDock
import com.frameout.designsystem.dock.FrameFab
import com.frameout.designsystem.glass.resolveGlassPipeline
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.FrameThemeMode
import com.frameout.designsystem.theme.GlassIntensity

@Composable
fun DesignGalleryScreen(onAddVideo: () -> Unit = {}) {
    var themeMode by remember { mutableStateOf(FrameThemeMode.Dark) }
    var glass by remember { mutableStateOf(GlassIntensity.Full) }
    var selectedDock by remember { mutableStateOf("library") }
    FrameTheme(mode = themeMode, glassIntensity = glass) {
        val colors = FrameTheme.colors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 100.dp)
            ) {
                FrameText("Design system", style = FrameTheme.typography.display)
                Spacer(Modifier.height(4.dp))
                FrameText(
                    "Pipeline: ${resolveGlassPipeline()} · Glass: $glass",
                    style = FrameTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                ThemeControls(themeMode, glass, { themeMode = it }, { glass = it })
                Spacer(Modifier.height(12.dp))
                GlassDemo()
                Spacer(Modifier.height(16.dp))
                IconsDemo()
                Spacer(Modifier.height(16.dp))
                TypeDemo()
            }
            FrameDock(
                items = listOf(
                    DockItem(
                        id = "library",
                        label = FrameStrings.library(),
                        icon = FrameIcons.Library,
                        selectedIcon = FrameIcons.LibraryFilled,
                        selected = selectedDock == "library",
                        onClick = { selectedDock = "library" }
                    ),
                    DockItem(
                        id = "settings",
                        label = FrameStrings.settings(),
                        icon = FrameIcons.Settings,
                        selectedIcon = FrameIcons.SettingsFilled,
                        selected = selectedDock == "settings",
                        onClick = { selectedDock = "settings" }
                    )
                ),
                fab = {
                    FrameFab(
                        icon = FrameIcons.VideoAdd,
                        contentDescription = FrameStrings.addVideo(),
                        onClick = onAddVideo
                    )
                }
            )
        }
    }
}
