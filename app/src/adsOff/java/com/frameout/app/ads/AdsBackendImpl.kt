package com.frameout.app.ads

import android.app.Activity
import android.content.Context

object AdsBackendImpl : AdsBackend {
    override val available: Boolean = false
    override fun gatherConsent(activity: Activity, done: (Boolean) -> Unit) = done(false)
    override fun privacyOptionsRequired(): Boolean = false
    override fun showPrivacyOptions(activity: Activity, done: () -> Unit) = done()
    override fun initialize(context: Context, ready: () -> Unit) = Unit
    override fun loadInterstitial(context: Context, unitId: String, result: (AdLoad) -> Unit) = Unit
    override fun interstitialReady(): Boolean = false
    override fun showInterstitial(activity: Activity, shown: () -> Unit, closed: (Boolean) -> Unit): Boolean = false
    override fun loadRewarded(context: Context, unitId: String, result: (AdLoad) -> Unit) = Unit
    override fun rewardedReady(): Boolean = false
    override fun showRewarded(activity: Activity, earned: () -> Unit, closed: () -> Unit): Boolean = false
    override fun preloadBanner(context: Context, unitId: String, widthDp: Int, result: (AdLoad) -> Unit) = Unit
    override fun bannerReady(): Boolean = false
    override fun bannerHeightDp(context: Context, widthDp: Int): Int = 0
    override fun setBannerActive(active: Boolean) = Unit
    override fun bindActivity(activity: Activity?) = Unit
}
