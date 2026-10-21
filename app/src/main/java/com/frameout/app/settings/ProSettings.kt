package com.frameout.app.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.FrameOutApp
import com.frameout.app.billing.BillingCatalog
import com.frameout.app.billing.CustomerCenterHost
import com.frameout.app.billing.RevenueCatSync
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
internal fun ProSettings() {
    val context = LocalContext.current
    val entitlements = (context.applicationContext as FrameOutApp).container.entitlements
    if (!entitlements.enforced) return
    val pro by entitlements.isPro.collectAsState()
    SettingsSection(ProStrings.settingsTitle()) {
        SettingsCard {
            if (pro) ActiveRow(context, entitlements.plan) else FreeRow { entitlements.showPaywall(ProFeature.General) }
        }
    }
}

@Composable
private fun FreeRow(onUpgrade: () -> Unit) {
    NavRow(FrameIcons.Star, ProStrings.settingsTitle(), hint = ProStrings.settingsFree(), onClick = onUpgrade)
}

@Composable
private fun ActiveRow(context: Context, plan: String) {
    var center by remember { mutableStateOf(false) }
    val managed = RevenueCatSync.isActive
    Column {
        NavRow(FrameIcons.StarFilled, ProStrings.active(), hint = ProStrings.activeBody()) {}
        if (managed || plan == "sub") {
            SettingsDivider()
            NavRow(FrameIconsExtra.Link, ProStrings.manage()) { if (managed) center = true else openSubscriptions(context) }
        }
    }
    if (center) CustomerCenterHost { center = false }
}

private fun openSubscriptions(context: Context) {
    val uri = Uri.parse("https://play.google.com/store/account/subscriptions?sku=${BillingCatalog.SUB_ID}&package=${context.packageName}")
    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
