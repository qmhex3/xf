package com.frameout.app
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.FrameThemeMode
import com.frameout.designsystem.theme.GlassIntensity
import com.frameout.designsystem.theme.rememberFrameAccent
@Composable
fun PreferredFrameTheme(content: @Composable () -> Unit) {
    val app = LocalContext.current.applicationContext as FrameOutApp
    val theme by app.container.prefs.themeMode.collectAsState()
    val glass by app.container.prefs.glassIntensity.collectAsState()
    val accentKey by app.container.prefs.accent.collectAsState()
    FrameTheme(
        mode = when (theme) {
            "Light" -> FrameThemeMode.Light
            "AMOLED" -> FrameThemeMode.Amoled
            "System" -> FrameThemeMode.System
            else -> FrameThemeMode.Dark
        },
        glassIntensity = when (glass) {
            "Simple", "Light" -> GlassIntensity.Light
            "Off" -> GlassIntensity.Off
            else -> GlassIntensity.Full
        },
        accent = rememberFrameAccent(accentKey),
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                com.frameout.designsystem.dock.LocalDockMoreLabel provides com.frameout.core.util.FrameStrings.more(),
                content = content
            )
        }
    )
}
