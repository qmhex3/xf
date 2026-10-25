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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var loadingInterstitial = false
    private var loadingRewarded = false
    private var failures = 0

    private val banner = MutableStateFlow(false)
    private val reward = MutableStateFlow(false)
    private val blocked = MutableStateFlow(false)
    private val privacy = MutableStateFlow(false)

    val bannerVisible: StateFlow<Boolean> = banner.asStateFlow()
    val rewardReady: StateFlow<Boolean> = reward.asStateFlow()
    val blockedNotice: StateFlow<Boolean> = blocked.asStateFlow()
    val privacyOptions: StateFlow<Boolean> = privacy.asStateFlow()

    init {
        governor.startSession(SystemClock.elapsedRealtime())
        scope.launch(Dispatchers.Main) { entitlements.isPro.collect { refresh() } }
    }

    fun attach(host: Activity) {
        activity = WeakReference(host)
        if (!BuildConfig.ADS_ENABLED || !backend.available || gathered) return
        gathered = true
        backend.gatherConsent(host) { allowed -> onConsent(allowed) }
    }

    fun detach(host: Activity) {
        if (activity?.get() === host) activity = null
    }

    fun setResumed(value: Boolean) {
        resumed = value
        if (value) preload()
    }

    fun noteAction() = governor.noteAction()

    fun active(): Boolean =
        BuildConfig.ADS_ENABLED && backend.available && entitlements.enforced && !entitlements.isPro.value

    fun dismissBlockedNotice() {
        blocked.value = false
    }

    fun showPrivacyOptions() {
        activity?.get()?.let { host -> backend.showPrivacyOptions(host) { refresh() } }
    }

    fun onBannerResult(result: AdLoad) = onLoad(result)

    private fun onConsent(allowed: Boolean) {
        consent = allowed
        privacy.value = backend.privacyOptionsRequired()
        if (allowed && !ready) backend.initialize(app) { ready = true; preload(); refresh() }
        refresh()
    }

    private fun refresh() {
        val on = active() && consent
        banner.value = on && health.state != AdState.Blocked
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
        scope.launch(Dispatchers.Main) { delay(wait); preload() }
    }

    private fun preload() {
        if (!active() || !consent || !ready) return
        if (!loadingInterstitial && !backend.interstitialReady()) {
            loadingInterstitial = true
            backend.loadInterstitial(app, BuildConfig.ADMOB_INTERSTITIAL_ID) { loadingInterstitial = false; onLoad(it) }
        }
        if (!loadingRewarded && !backend.rewardedReady()) {
            loadingRewarded = true
            backend.loadRewarded(app, BuildConfig.ADMOB_REWARDED_ID) { loadingRewarded = false; onLoad(it); refresh() }
        }
    }

    fun onNaturalBreak(placement: AdPlacement) {
        val verdict = governor.check(placement, active(), consent && ready, tick(), now())
        if (verdict != AdVerdict.Allow || !resumed) return
        if (!backend.interstitialReady()) { preload(); return }
        scope.launch(Dispatchers.Main) { delay(SHOW_DELAY_MS); showInterstitial() }
    }

    private fun showInterstitial() {
        val host = activity?.get() ?: return
        if (!resumed || !active()) return
        val started = backend.showInterstitial(host) { preload() }
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
    }
}
