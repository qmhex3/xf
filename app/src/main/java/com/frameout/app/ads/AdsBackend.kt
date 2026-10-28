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

    /**
     * [shown] runs when the ad is on screen. [closed] runs once when it is gone; its argument is true only if the
     * ad was really displayed and dismissed (false when it failed to show, so the user still owes it).
     */
    fun showInterstitial(activity: Activity, shown: () -> Unit, closed: (Boolean) -> Unit): Boolean
    fun loadRewarded(context: Context, unitId: String, result: (AdLoad) -> Unit)
    fun rewardedReady(): Boolean
    fun showRewarded(activity: Activity, earned: () -> Unit, closed: () -> Unit): Boolean

    /** Banner kept alive at app level so it is already loaded when a screen shows it again. */
    fun preloadBanner(context: Context, unitId: String, widthDp: Int, result: (AdLoad) -> Unit)
    fun bannerReady(): Boolean
    fun bannerHeightDp(context: Context, widthDp: Int): Int
    fun setBannerActive(active: Boolean)

    /** Gives the cached banner the current Activity (needed for ad clicks) and releases it when null. */
    fun bindActivity(activity: Activity?)
}
