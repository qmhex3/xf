package com.frameout.app.billing

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.frameout.app.BuildConfig
import com.frameout.app.FrameOutApp
import com.frameout.core.pro.ProFeature

@Composable
fun PaywallHost(activity: Activity, app: FrameOutApp) {
    val entitlements = app.container.entitlements
    val feature by entitlements.paywall.collectAsState()
    val pro by entitlements.isPro.collectAsState()
    val manager = app.billing
    LaunchedEffect(feature) { if (feature != null) manager.start() }
    var celebrating by remember { mutableStateOf(false) }
    var wasPro by remember { mutableStateOf(pro) }
    LaunchedEffect(pro) {
        if (pro && !wasPro && feature != null) celebrating = true else if (pro && feature != null) entitlements.dismissPaywall()
        wasPro = pro
    }
    if (celebrating) ProUnlockedDialog { celebrating = false; entitlements.dismissPaywall() }
    feature?.let { shown ->
        if (BuildConfig.REVENUECAT_PAYWALL && RevenueCatSync.isActive) {
            RevenueCatPaywall(onDismiss = entitlements::dismissPaywall)
        } else {
            CompositionLocalProvider(LocalPaywallSample provides PaywallSample.image) {
                PaywallSheet(
                    feature = shown,
                    trialsLeft = if (shown == ProFeature.General) null else entitlements.trialsLeft(shown),
                    manager = manager,
                    onLaunch = { offer -> manager.launch(activity, offer) },
                    onDismiss = entitlements::dismissPaywall
                )
            }
        }
    }
}
