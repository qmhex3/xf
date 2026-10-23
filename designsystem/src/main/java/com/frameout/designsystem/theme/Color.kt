package com.frameout.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private const val LightSurfaceTint = 0.05f

object FrameColors {
    val DarkBackground = Color(0xFF0A0E0D)
    val DarkSurface = Color(0xFF101514)
    val DarkContainer = Color(0xFF161C1B)
    val DarkContainerHigh = Color(0xFF1E2625)
    val DarkOutline = Color(0xFF33403E)
    val DarkOnSurface = Color(0xFFE9EFED)
    val DarkOnSurfaceVariant = Color(0xFFA7B4B0)
    val DarkPrimary = Color(0xFF9A9DFF)
    val DarkOnPrimary = Color(0xFF0B0A5C)
    val DarkPrimaryContainer = Color(0xFF2A2C7A)
    val DarkSuccess = Color(0xFF3DDC97)
    val DarkWarning = Color(0xFFFFB84D)
    val DarkError = Color(0xFFFF7078)
    val ToastError = Color(0xFFFF453A)

    val LightBackground = Color(0xFFE6E8F1)
    val LightSurface = Color(0xFFF2F3F9)
    val LightContainer = Color(0xFFDADDEA)
    val LightContainerHigh = Color(0xFFCED2E3)
    val LightOutline = Color(0xFF9096B0)
    val LightOnSurface = Color(0xFF191B28)
    val LightOnSurfaceVariant = Color(0xFF4E5268)
    val LightPrimary = Color(0xFF4347C9)
    val LightOnPrimary = Color(0xFFF7F7FF)
    val LightPrimaryContainer = Color(0xFFD5D6F8)
    val LightSuccess = Color(0xFF0B6B4D)
    val LightWarning = Color(0xFF9A4A06)
    val LightError = Color(0xFFB3261E)

    val AmoledBackground = Color(0xFF000000)
    val NeutralBackground = Color(0xFF0F0F0F)
    val NeutralSurface = Color(0xFF151515)
    val NeutralContainer = Color(0xFF1C1C1C)
    val NeutralContainerHigh = Color(0xFF262626)
    val NeutralOutline = Color(0xFF383838)
    val NeutralBlackSurface = Color(0xFF0B0B0B)
    val NeutralBlackContainer = Color(0xFF141414)
    val NeutralBlackContainerHigh = Color(0xFF1D1D1D)
    val NeutralBlackOutline = Color(0xFF2C2C2C)
    val NeutralOnSurface = Color(0xFFEDEDED)
    val NeutralOnSurfaceVariant = Color(0xFFA0A0A0)
    val ViewerBlack = Color(0xFF000000)
}

data class FrameColorScheme(
    val background: Color,
    val surface: Color,
    val container: Color,
    val containerHigh: Color,
    val outline: Color,
    val divider: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val accentA: Color,
    val accentB: Color,
    val accentC: Color,
    val onDarkAccent: Color,
    val isDark: Boolean,
    val neutral: Boolean = false
)

fun darkColorScheme(amoled: Boolean = false, accent: FrameAccent = FrameAccents.Indigo): FrameColorScheme {
    val neutral = accent.neutral
    fun grey(c: Color): Color { val g = c.red * 0.2126f + c.green * 0.7152f + c.blue * 0.0722f; return Color(g, g, g, c.alpha) }
    fun tint(base: Color) = lerp(grey(base), accent.darkContainer, accent.surfaceTint)
    fun ramp(base: Color, soft: Color, black: Color) = when {
        !neutral -> tint(base)
        amoled -> black
        else -> soft
    }
    return FrameColorScheme(
        background = when {
            amoled -> FrameColors.AmoledBackground
            neutral -> FrameColors.NeutralBackground
            else -> tint(FrameColors.DarkBackground)
        },
        surface = ramp(FrameColors.DarkSurface, FrameColors.NeutralSurface, FrameColors.NeutralBlackSurface),
        container = ramp(FrameColors.DarkContainer, FrameColors.NeutralContainer, FrameColors.NeutralBlackContainer),
        containerHigh = ramp(FrameColors.DarkContainerHigh, FrameColors.NeutralContainerHigh, FrameColors.NeutralBlackContainerHigh),
        outline = ramp(FrameColors.DarkOutline, FrameColors.NeutralOutline, FrameColors.NeutralBlackOutline),
        divider = ramp(FrameColors.DarkOutline, FrameColors.NeutralOutline, FrameColors.NeutralBlackOutline),
        onSurface = if (neutral) FrameColors.NeutralOnSurface else FrameColors.DarkOnSurface,
        onSurfaceVariant = if (neutral) FrameColors.NeutralOnSurfaceVariant else FrameColors.DarkOnSurfaceVariant,
        primary = accent.darkPrimary,
        onPrimary = accent.darkOnPrimary,
        primaryContainer = accent.darkContainer,
        success = FrameColors.DarkSuccess,
        warning = FrameColors.DarkWarning,
        error = FrameColors.DarkError,
        accentA = accent.a,
        accentB = accent.b,
        accentC = accent.c,
        onDarkAccent = accent.darkPrimary,
        isDark = true,
        neutral = neutral
    )
}

fun lightColorScheme(accent: FrameAccent = FrameAccents.Indigo): FrameColorScheme {
    fun tint(base: Color) = lerp(base, accent.a, LightSurfaceTint)
    return FrameColorScheme(
        background = tint(FrameColors.LightBackground),
        surface = tint(FrameColors.LightSurface),
        container = tint(FrameColors.LightContainer),
        containerHigh = tint(FrameColors.LightContainerHigh),
        outline = FrameColors.LightOutline.copy(alpha = 0.32f),
        divider = FrameColors.LightOutline.copy(alpha = 0.32f),
        onSurface = FrameColors.LightOnSurface,
        onSurfaceVariant = FrameColors.LightOnSurfaceVariant,
        primary = accent.lightPrimary,
        onPrimary = accent.lightOnPrimary,
        primaryContainer = accent.lightContainer,
        success = FrameColors.LightSuccess,
        warning = FrameColors.LightWarning,
        error = FrameColors.LightError,
        accentA = accent.a,
        accentB = accent.b,
        accentC = accent.c,
        onDarkAccent = accent.darkPrimary,
        isDark = false,
        neutral = accent.neutral
    )
}

fun lerpScheme(a: FrameColorScheme, b: FrameColorScheme, t: Float): FrameColorScheme = when {
    t <= 0f -> a
    t >= 1f -> b
    else -> FrameColorScheme(
        background = lerp(a.background, b.background, t),
        surface = lerp(a.surface, b.surface, t),
        container = lerp(a.container, b.container, t),
        containerHigh = lerp(a.containerHigh, b.containerHigh, t),
        outline = lerp(a.outline, b.outline, t),
        divider = lerp(a.divider, b.divider, t),
        onSurface = lerp(a.onSurface, b.onSurface, t),
        onSurfaceVariant = lerp(a.onSurfaceVariant, b.onSurfaceVariant, t),
        primary = lerp(a.primary, b.primary, t),
        onPrimary = lerp(a.onPrimary, b.onPrimary, t),
        primaryContainer = lerp(a.primaryContainer, b.primaryContainer, t),
        success = lerp(a.success, b.success, t),
        warning = lerp(a.warning, b.warning, t),
        error = lerp(a.error, b.error, t),
        accentA = lerp(a.accentA, b.accentA, t),
        accentB = lerp(a.accentB, b.accentB, t),
        accentC = lerp(a.accentC, b.accentC, t),
        onDarkAccent = lerp(a.onDarkAccent, b.onDarkAccent, t),
        isDark = if (t < 0.5f) a.isDark else b.isDark,
        neutral = if (t < 0.5f) a.neutral else b.neutral
    )
}

val FrameColorScheme.fieldFill: Color
    get() = if (isDark) Color.White.copy(alpha = 0.06f) else container
