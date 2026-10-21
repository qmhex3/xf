package com.frameout.app.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.FrameOutApp
import com.frameout.core.pro.Entitlements
import com.frameout.core.pro.ProFeature

@Stable
class ProGate(private val entitlements: Entitlements, val isPro: Boolean) {
    val enforced: Boolean get() = entitlements.enforced

    fun locked(free: Boolean): Boolean = enforced && !isPro && !free

    fun run(feature: ProFeature, item: String = "", action: () -> Unit) {
        if (entitlements.tryOnce(feature, item)) action() else entitlements.showPaywall(feature)
    }

    fun require(feature: ProFeature, action: () -> Unit) {
        if (entitlements.permits(feature)) action() else entitlements.showPaywall(feature)
    }

    fun gated(feature: ProFeature, free: Boolean, item: String = "", action: () -> Unit) {
        if (free) action() else run(feature, item, action)
    }
}

@Composable
fun rememberProGate(): ProGate {
    val app = LocalContext.current.applicationContext as FrameOutApp
    val entitlements = app.container.entitlements
    val pro by entitlements.isPro.collectAsState()
    return remember(entitlements, pro) { ProGate(entitlements, pro) }
}
