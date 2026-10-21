package com.frameout.designsystem.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.frameout.designsystem.theme.GlassIntensity

enum class GlassPipeline {
    Agsl,
    RenderEffect,
    Fallback
}

fun resolveGlassPipeline(): GlassPipeline {
    return when {
        Build.VERSION.SDK_INT >= 33 -> GlassPipeline.Agsl
        Build.VERSION.SDK_INT >= 31 -> GlassPipeline.RenderEffect
        else -> GlassPipeline.Fallback
    }
}

fun glassDisabled(intensity: GlassIntensity, powerSave: Boolean, lowRam: Boolean, reduceTransparency: Boolean, thermalThrottled: Boolean = false): Boolean =
    intensity == GlassIntensity.Off || powerSave || lowRam || reduceTransparency || thermalThrottled

fun thermalThrottled(power: PowerManager?): Boolean =
    Build.VERSION.SDK_INT >= 29 && power != null && power.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE

fun shouldDisableGlass(context: Context, intensity: GlassIntensity, reduceTransparency: Boolean = false): Boolean {
    val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return glassDisabled(intensity, power?.isPowerSaveMode == true, am?.isLowRamDevice == true, reduceTransparency, thermalThrottled(power))
}

fun glassFillAlpha(intensity: GlassIntensity, pipeline: GlassPipeline): Float {
    if (intensity == GlassIntensity.Off) return 0.92f
    val base = when (pipeline) {
        GlassPipeline.Agsl -> 0.12f
        GlassPipeline.RenderEffect -> 0.14f
        GlassPipeline.Fallback -> 0.72f
    }
    return if (intensity == GlassIntensity.Light) base * 0.6f else base
}

fun glassBorderAlpha(intensity: GlassIntensity): Float {
    return when (intensity) {
        GlassIntensity.Full -> 0.18f
        GlassIntensity.Light -> 0.10f
        GlassIntensity.Off -> 0.08f
    }
}
