package com.frameout.app.ads

import android.app.Activity
import android.content.Context

data class AdLoad(val ok: Boolean, val code: Int = 0)

interface AdsBackend {
    val available: Boolean
    fun gatherConsent(activity: Activity, done: (Boolean) -> Unit)
    fun privacyOptionsRequired(): Boolean
    fun showPrivacyOptions(activity: Activity, done: () -> Unit)
    fun initialize(context: Context, ready: () -> Unit)
    fun loadInterstitial(context: Context, unitId: String, result: (AdLoad) -> Unit)
    fun interstitialReady(): Boolean
    fun showInterstitial(activity: Activity, closed: () -> Unit): Boolean
    fun loadRewarded(context: Context, unitId: String, result: (AdLoad) -> Unit)
    fun rewardedReady(): Boolean
    fun showRewarded(activity: Activity, earned: () -> Unit, closed: () -> Unit): Boolean
}
