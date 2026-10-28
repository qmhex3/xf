package com.frameout.app.ads

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import com.frameout.app.BuildConfig
import com.frameout.core.ads.AdGovernor
import com.frameout.core.ads.AdHealth
import com.frameout.core.ads.AdPlacement
import com.frameout.core.ads.AdRules
import com.frameout.core.ads.AdState
import com.frameout.core.ads.AdVerdict
import com.frameout.core.ads.CountryTiers
import com.frameout.core.pro.Entitlements
import com.frameout.core.pro.ProFeature
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BannerState { Hidden, Loading, Ready }

class AdsHub(
    private val app: Application,
    private val entitlements: Entitlements,
    private val scope: CoroutineScope,
    private val backend: AdsBackend = AdsBackendImpl
) {
    private val store = PrefsAdStore(app)
    private val governor = AdGovernor(store, AdRules.forTier(CountryTiers.of(AdsCountry.iso(app))))
    private val health = AdHealth(store)
    private var activity: WeakReference<Activity>? = null
    private var resumed = false
    private var consent = false
    private var ready = false
    private var gathered = false
    private var initializing = false
    private var gatherAttempts = 0
    private var lastGatherAt = 0L
    private var loadingInterstitial = false
    private var loadingRewarded = false
    private var loadingBanner = false
    private var bannerFailed = false
    private var showPending = false
    private var failures = 0
    private var retryJob: Job? = null
    private var expiryJob: Job? = null

    private val banner = MutableStateFlow(BannerState.Hidden)
    private val reward = MutableStateFlow(false)
    private val blocked = MutableStateFlow(false)
    private val privacy = MutableStateFlow(false)

    val bannerState: StateFlow<BannerState> = banner.asStateFlow()
    val rewardReady: StateFlow<Boolean> = reward.asStateFlow()
    val blockedNotice: StateFlow<Boolean> = blocked.asStateFlow()
    val privacyOptions: StateFlow<Boolean> = privacy.asStateFlow()

    init {
        governor.startSession(SystemClock.elapsedRealtime())
        scope.launch(Dispatchers.Main) { entitlements.isPro.collect { refresh() } }
    }

    fun attach(host: Activity) {
        activity = WeakReference(host)
        backend.bindActivity(host)
        if (!BuildConfig.ADS_ENABLED || !backend.available || gathered) return
        gather(host)
    }

    private fun gather(host: Activity) {
        gathered = true
        gatherAttempts++
        lastGatherAt = tick()
        backend.gatherConsent(host) { allowed -> onConsent(allowed) }
    }

    fun detach(host: Activity) {
        if (activity?.get() === host) {
            activity = null
            backend.bindActivity(null)
        }
    }

    fun setResumed(value: Boolean) {
        resumed = value
        backend.setBannerActive(value)
        if (!value) return
        retryConsent()
        preload()
    }

    /** A failed consent request (for example no network on the very first launch) must not disable ads until restart. */
    private fun retryConsent() {
        if (consent || !gathered || gatherAttempts >= MAX_GATHER || tick() - lastGatherAt < GATHER_GAP_MS) return
        if (!BuildConfig.ADS_ENABLED || !backend.available) return
        activity?.get()?.let { gather(it) }
    }

    fun bannerHeightDp(widthDp: Int): Int = backend.bannerHeightDp(app, widthDp)

    fun noteAction() = governor.noteAction()

    fun active(): Boolean =
        BuildConfig.ADS_ENABLED && backend.available && entitlements.enforced && !entitlements.isPro.value

    fun dismissBlockedNotice() {
        blocked.value = false
    }

    fun showPrivacyOptions() {
        activity?.get()?.let { host -> backend.showPrivacyOptions(host) { refresh() } }
    }

    fun onBannerResult(result: AdLoad) {
        loadingBanner = false
        bannerFailed = !result.ok
        onLoad(result)
    }

    private fun onConsent(allowed: Boolean) {
        consent = allowed
        privacy.value = backend.privacyOptionsRequired()
        if (allowed && !ready && !initializing) {
            initializing = true
            backend.initialize(app) { ready = true; initializing = false; preload(); refresh() }
        }
        refresh()
    }

    private fun refresh() {
        val on = active() && consent
        banner.value = when {
            !on || !ready || health.state == AdState.Blocked -> BannerState.Hidden
            backend.bannerReady() -> BannerState.Ready
            bannerFailed -> BannerState.Hidden
            else -> BannerState.Loading
        }
        reward.value = on && ready && backend.rewardedReady()
        if (!entitlements.isPro.value) return
        blocked.value = false
    }

    private fun now() = System.currentTimeMillis()

    private fun tick() = SystemClock.elapsedRealtime()

    private fun onLoad(result: AdLoad) {
        if (result.ok) {
            failures = 0
            health.onLoaded(now())
            refresh()
            return
        }
        failures++
        scope.launch(Dispatchers.IO) {
            val online = AdBlockProbe.online(app)
            val sink = online && AdBlockProbe.adDnsSinkholed()
            withContext(Dispatchers.Main) { afterFailure(result.code, online, sink) }
        }
    }

    private fun afterFailure(code: Int, online: Boolean, sink: Boolean) {
        health.onFailed(code, online, sink, now())
        refresh()
        if (resumed && active() && health.shouldNotify(now())) {
            health.noticeShown(now())
            blocked.value = true
        }
        val wait = minOf(30_000L shl minOf(failures, 5), 600_000L)
        retryJob?.cancel()
        retryJob = scope.launch(Dispatchers.Main) { delay(wait); preload() }
    }

    /** Keeps one interstitial, one rewarded ad and one banner ready so nothing waits for the network at show time. */
    private fun preload() {
        if (!active() || !consent || !ready) return
        if (!loadingInterstitial && !backend.interstitialReady()) {
            loadingInterstitial = true
            backend.loadInterstitial(app, BuildConfig.ADMOB_INTERSTITIAL_ID) { loadingInterstitial = false; onFullScreenLoad(it) }
        }
        if (!loadingRewarded && !backend.rewardedReady()) {
            loadingRewarded = true
            backend.loadRewarded(app, BuildConfig.ADMOB_REWARDED_ID) { loadingRewarded = false; onFullScreenLoad(it); refresh() }
        }
        if (!loadingBanner && !backend.bannerReady()) {
            loadingBanner = true
            backend.preloadBanner(app, BuildConfig.ADMOB_BANNER_ID, app.resources.configuration.screenWidthDp, ::onBannerResult)
        }
        refresh()
    }

    private fun onFullScreenLoad(result: AdLoad) {
        onLoad(result)
        if (result.ok) scheduleExpiry()
    }

    /** Loaded ads expire after about an hour; reload just after that while the app is in front. */
    private fun scheduleExpiry() {
        expiryJob?.cancel()
        expiryJob = scope.launch(Dispatchers.Main) { delay(EXPIRY_RELOAD_MS); if (resumed) preload() }
    }

    fun onNaturalBreak(placement: AdPlacement) {
        val verdict = governor.check(placement, active(), consent && ready, tick(), now())
        if (verdict != AdVerdict.Allow || !resumed) return
        if (!backend.interstitialReady()) { preload(); return }
        if (showPending) return
        showPending = true
        scope.launch(Dispatchers.Main) { delay(SHOW_DELAY_MS); showPending = false; showInterstitial() }
    }

    /**
     * An ad that became due stays owed (see [AdGovernor.owes]) until the user has seen it through to the dismiss
     * button. If the app is closed, the screen changes or the ad fails to show, it is simply asked for again at the
     * next natural break, in this session or a later one.
     */
    private fun showInterstitial() {
        val host = activity?.get() ?: return
        if (!resumed || !active()) return
        val started = backend.showInterstitial(host, {}) { completed ->
            if (completed) governor.settle()
            preload()
        }
        if (started) governor.recordShown(tick(), now())
    }

    fun rewardAvailable(): Boolean = reward.value && governor.canGrantReward(now())

    fun showReward(feature: ProFeature, finished: (Boolean) -> Unit) {
        val host = activity?.get()
        if (host == null || !rewardAvailable()) { finished(false); return }
        var earned = false
        val started = backend.showRewarded(host, {
            earned = true
            governor.recordReward(now())
            entitlements.grantTicket(feature, governor.rules.rewardTicketMs)
        }) { finished(earned); preload(); refresh() }
        if (!started) finished(false)
    }

    private companion object {
        const val SHOW_DELAY_MS = 1_200L
        const val EXPIRY_RELOAD_MS = 55 * 60_000L + 5_000L
        const val GATHER_GAP_MS = 5 * 60_000L
        const val MAX_GATHER = 4
    }
}
