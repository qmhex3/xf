package com.frameout.app.onboarding

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.frameout.app.oem.OemSettings
import com.frameout.core.media.PermissionHelper
import com.frameout.core.util.DeviceCapability

internal data class ReadyPermissions(
    val media: Boolean,
    val notifications: Boolean,
    val battery: Boolean,
    val oem: String,
    val requestMedia: () -> Unit,
    val requestNotifications: () -> Unit,
    val requestBattery: () -> Unit,
    val openBatteryList: () -> Unit
)

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun batteryUnrestricted(context: Context): Boolean =
    runCatching { (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName) }
        .getOrDefault(false)

@Composable
private fun rememberResumeTick(context: Context): Int {
    var tick by remember { mutableIntStateOf(0) }
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) tick++ }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }
    return tick
}

@Composable
internal fun rememberReadyPermissions(onAllowNotifications: () -> Unit): ReadyPermissions {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val version = rememberResumeTick(context) + refresh
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++ }
    val oem = remember(context) { DeviceCapability(context).oemFamily }
    return ReadyPermissions(
        media = remember(version) { PermissionHelper.hasMediaAccess(context) },
        notifications = remember(version) { notificationsGranted(context) },
        battery = remember(version) { batteryUnrestricted(context) },
        oem = oem,
        requestMedia = { mediaLauncher.launch(PermissionHelper.mediaPermissions()) },
        requestNotifications = { onAllowNotifications(); refresh++ },
        requestBattery = { OemSettings.openBatteryList(context) },
        openBatteryList = { OemSettings.openAutostartOrBattery(context, oem) }
    )
}
