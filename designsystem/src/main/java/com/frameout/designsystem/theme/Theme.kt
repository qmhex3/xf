package com.frameout.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.rememberSystemA11y

enum class FrameThemeMode {
    Dark,
    Light,
    System,
    Amoled
}

enum class GlassIntensity {
    Full,
    Light,
    Off
}

data class FrameThemeState(
    val colors: FrameColorScheme,
    val typography: FrameTypography,
    val glassIntensity: GlassIntensity,
    val accent: FrameAccent = FrameAccents.Indigo
)

val LocalFrameTheme = compositionLocalOf {
    FrameThemeState(
        colors = darkColorScheme(),
        typography = frameTypography(),
        glassIntensity = GlassIntensity.Full
    )
}

object FrameTheme {
    val colors: FrameColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalFrameTheme.current.colors

    val typography: FrameTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalFrameTheme.current.typography

    val glassIntensity: GlassIntensity
        @Composable
        @ReadOnlyComposable
        get() = LocalFrameTheme.current.glassIntensity

    val accent: FrameAccent
        @Composable
        @ReadOnlyComposable
        get() = LocalFrameTheme.current.accent
}

@Composable
fun FrameTheme(
    mode: FrameThemeMode = FrameThemeMode.Dark,
    glassIntensity: GlassIntensity = GlassIntensity.Full,
    accent: FrameAccent = FrameAccents.Indigo,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        FrameThemeMode.Dark, FrameThemeMode.Amoled -> true
        FrameThemeMode.Light -> false
        FrameThemeMode.System -> isSystemInDarkTheme()
    }
    val amoled = mode == FrameThemeMode.Amoled
    val target = remember(dark, amoled, accent) {
        if (dark) darkColorScheme(amoled, accent) else lightColorScheme(accent)
    }
    val systemA11y = rememberSystemA11y()
    val colors = rememberCrossfadedScheme(target, systemA11y.reduceMotion)
    val typography = remember { frameTypography() }
    val state = FrameThemeState(
        colors = colors,
        typography = typography,
        glassIntensity = glassIntensity,
        accent = accent
    )
    SyncSystemBars(target.isDark, target.background)
    CompositionLocalProvider(
        LocalFrameTheme provides state,
        LocalSystemA11y provides systemA11y,
        androidx.compose.foundation.LocalIndication provides FramePressIndication
    ) {
        content()
    }
}

@Composable
fun contentColorFor(background: Color): Color {
    val scheme = FrameTheme.colors
    return ContrastGuard.readableOn(background, listOf(scheme.onPrimary, scheme.onSurface))
}

private const val THEME_FADE_MS = 240
private val ThemeEasing = CubicBezierEasing(0.33f, 0f, 0.2f, 1f)

@Composable
private fun rememberCrossfadedScheme(target: FrameColorScheme, reduceMotion: Boolean): FrameColorScheme {
    val progress = remember { Animatable(1f) }
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        from = lerpScheme(from, to, progress.value)
        to = target
        if (reduceMotion) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(THEME_FADE_MS, easing = ThemeEasing))
        }
    }
    return lerpScheme(from, to, progress.value)
}

@Composable
private fun SyncSystemBars(dark: Boolean, background: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val applied = remember { intArrayOf(-1, 0) }
    SideEffect {
        val argb = background.toArgb()
        val flag = if (dark) 1 else 0
        if (applied[0] == flag && applied[1] == argb) return@SideEffect
        applied[0] = flag
        applied[1] = argb
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        window.setBackgroundDrawable(ColorDrawable(argb))
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
