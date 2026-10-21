package com.frameout.app.oem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

internal object OemSettings {
    fun isCustom(oem: String): Boolean = oem != "stock"

    fun steps(oem: String): String {
        val key = oem.lowercase()
        return when {
            key.contains("xiaomi") || key.contains("redmi") || key.contains("poco") -> FrameStrings.oemStepsXiaomi()
            key.contains("huawei") || key.contains("honor") -> FrameStrings.oemStepsHuawei()
            key.contains("oppo") || key.contains("realme") || key.contains("oneplus") -> FrameStrings.oemStepsOppo()
            key.contains("vivo") || key.contains("iqoo") -> FrameStrings.oemStepsVivo()
            key.contains("samsung") -> FrameStrings.oemStepsSamsung()
            else -> FrameStrings.oemHint()
        }
    }

    fun openAutostartOrBattery(context: Context, oem: String) {
        val key = oem.lowercase()
        val intents = mutableListOf<Intent>()
        when {
            key.contains("xiaomi") || key.contains("redmi") || key.contains("poco") -> {
                intents += Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
                intents += Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").addCategory(Intent.CATEGORY_DEFAULT)
            }
            key.contains("huawei") || key.contains("honor") -> {
                intents += Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"))
                intents += Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"))
            }
            key.contains("oppo") || key.contains("realme") -> {
                intents += Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"))
                intents += Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerControlActivity"))
            }
            key.contains("vivo") -> {
                intents += Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
            }
            key.contains("samsung") -> {
                if (Build.VERSION.SDK_INT >= 28) {
                    intents += Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                }
            }
        }
        intents += Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        intents += Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.fromParts("package", context.packageName, null))
        start(context, intents)
    }

    fun openBatteryList(context: Context) {
        start(
            context,
            listOf(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.fromParts("package", context.packageName, null))
            )
        )
    }

    private fun start(context: Context, intents: List<Intent>) {
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
internal fun OemStepsCard(oem: String, modifier: Modifier = Modifier, onOpenBattery: (() -> Unit)? = null) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.md
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.containerHigh.copy(alpha = 0.5f))
            .border(1.dp, frameHairline(), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Image(
                painter = painterResource(FrameIcons.Info),
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp).size(16.dp),
                colorFilter = ColorFilter.tint(colors.primary)
            )
            FrameText(
                OemSettings.steps(oem),
                style = FrameTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        if (onOpenBattery != null) {
            FrameText(
                FrameStrings.onboardingOemBatteryLink(),
                style = FrameTheme.typography.titleSmall.copy(fontSize = 12.sp),
                color = colors.primary,
                modifier = Modifier.frameClickable(onClick = onOpenBattery)
            )
        }
    }
}
