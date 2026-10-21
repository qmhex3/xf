package com.frameout.app.billing

import com.frameout.core.pro.ProAccess
import com.frameout.core.pro.ProFeature

object ProCheck {
    fun locked(free: Boolean): Boolean {
        val entitlements = ProAccess.source ?: return !free
        return entitlements.enforced && !entitlements.isPro.value && !free
    }

    fun pass(feature: ProFeature, item: String = ""): Boolean {
        val entitlements = ProAccess.source ?: return false
        if (!entitlements.enforced || entitlements.tryOnce(feature, item)) return true
        entitlements.showPaywall(feature)
        return false
    }
}
