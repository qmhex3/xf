package com.frameout.designsystem.a11y

import android.app.UiModeManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

data class SystemA11y(
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false
)

val LocalSystemA11y = staticCompositionLocalOf { SystemA11y() }

private const val HIGH_TEXT_CONTRAST = "high_text_contrast_enabled"
private const val COLOR_INVERSION = "accessibility_display_inversion_enabled"
private const val HIGH_CONTRAST_LEVEL = 0.5f

fun readSystemA11y(context: Context): SystemA11y =
    SystemA11y(reduceMotion = false, reduceTransparency = isTransparencyReduced(context))

fun isMotionReduced(context: Context): Boolean {
    val resolver = context.contentResolver
    val animator = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transition = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    return animator == 0f || transition == 0f
}

fun isTransparencyReduced(context: Context): Boolean {
    val resolver = context.contentResolver
    val highText = Settings.Secure.getInt(resolver, HIGH_TEXT_CONTRAST, 0) == 1
    val inverted = Settings.Secure.getInt(resolver, COLOR_INVERSION, 0) == 1
    return highText || inverted || isHighContrastTheme(context)
}

private fun isHighContrastTheme(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 34) return false
    val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return (uiMode?.contrast ?: 0f) >= HIGH_CONTRAST_LEVEL
}

private fun observedUris(): List<Uri> = listOf(
    Settings.Secure.getUriFor(HIGH_TEXT_CONTRAST),
    Settings.Secure.getUriFor(COLOR_INVERSION)
)

@Composable
fun rememberSystemA11y(): SystemA11y {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val state = remember(configuration) { mutableStateOf(readSystemA11y(context)) }
    DisposableEffect(state) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                state.value = readSystemA11y(context)
            }
        }
        val resolver = context.contentResolver
        observedUris().forEach { resolver.registerContentObserver(it, false, observer) }
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return state.value
}
