package com.frameout.core.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

class DeviceCapability(context: Context) {
    private val app = context.applicationContext
    private val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager

    val isLowRam: Boolean get() = am.isLowRamDevice

    val isPowerSave: Boolean get() = pm.isPowerSaveMode

    val thermalStatus: Int
        get() = if (Build.VERSION.SDK_INT >= 29) pm.currentThermalStatus else 0

    val isThermalThrottled: Boolean
        get() = thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE

    val maxConcurrentDecodes: Int
        get() = when {
            isLowRam || isThermalThrottled -> 1
            isPowerSave -> 1
            else -> 2
        }

    val maxThumbDecodes: Int
        get() = when {
            isLowRam || isThermalThrottled -> 1
            isPowerSave -> 2
            else -> 4
        }

    val poolBytes: Long
        get() = when {
            isLowRam -> 8L * 1024L * 1024L
            isThermalThrottled || isPowerSave -> 16L * 1024L * 1024L
            else -> 32L * 1024L * 1024L
        }

    val analysisReduced: Boolean get() = isLowRam || isThermalThrottled

    val scanCooldownMs: Long
        get() = when {
            thermalStatus >= PowerManager.THERMAL_STATUS_CRITICAL -> 3_000L
            isThermalThrottled -> 1_000L
            isPowerSave -> 250L
            else -> 0L
        }

    val preferLiteGlass: Boolean
        get() = isLowRam || isPowerSave || isThermalThrottled

    val oemFamily: String
        get() {
            val m = Build.MANUFACTURER.lowercase()
            return when {
                m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> "xiaomi"
                m.contains("oppo") || m.contains("realme") || m.contains("oneplus") -> "oppo"
                m.contains("huawei") || m.contains("honor") -> "huawei"
                m.contains("vivo") || m.contains("iqoo") -> "vivo"
                else -> "stock"
            }
        }
}
