package com.frameout.app

import android.app.Application
import com.frameout.app.billing.BillingManager
import com.frameout.core.hdr.HdrSupport
import com.frameout.core.util.AppContainer
import com.frameout.engine.decode.HdrFrameFetcher
import com.frameout.core.util.StringSource
import com.frameout.core.storage.StorageManager
import com.frameout.engine.work.ExtractReconciler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FrameOutApp : Application() {
    private val maintenance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var container: AppContainer
        private set
    lateinit var billing: BillingManager
        private set
    lateinit var ads: com.frameout.app.ads.AdsHub
        private set

    override fun onCreate() {
        super.onCreate()
        StringSource.init(this)
        com.frameout.core.util.DiagLog.init(this)
        com.frameout.core.quality.ArabicOcr.init(this)
        val firstInstallTime = runCatching {
            packageManager.getPackageInfo(packageName, 0).firstInstallTime
        }.getOrDefault(Long.MAX_VALUE)
        val existingInstall = firstInstallTime < BuildConfig.PAYWALL_CUTOFF_MS
        container = AppContainer(this, BuildConfig.PLAY_BILLING, existingInstall && BuildConfig.GRANDFATHER)
        if (BuildConfig.PLAY_BILLING) com.frameout.app.billing.RevenueCatSync.configure(this)
        billing = BillingManager(container.entitlements, appScope)
        ads = com.frameout.app.ads.AdsHub(this, container.entitlements, appScope)
        com.frameout.app.ads.AdsGateway.hub = ads
        if (BuildConfig.PLAY_BILLING) {
            billing.start()
        }
        com.frameout.core.storage.SaveFolderGuard.onNotice = { container.notice.value = it }
        com.frameout.app.security.Sentinel.install(this)
        container.rating.noteSession()
        HdrSupport.source = HdrFrameFetcher(this)
        com.frameout.core.storage.AnimatedFrameLookup.provider = com.frameout.engine.decode.AnimatedSources.lookup(this)
        com.frameout.core.media.VideoThumbLookup.provider = com.frameout.engine.decode.VideoThumbSources.lookup(this)
        com.frameout.core.media.FolderMediaIndex.prewarm(this)
        com.frameout.core.media.MediaPrewarm.start(this)
        val storage = StorageManager(this)
        maintenance.launch { runCatching { ExtractReconciler.reconcile(this@FrameOutApp) } }
        maintenance.launch { storage.cleanupTemporaryFiles(); storage.cleanupShareCache(); storage.cleanupMotionCache(); storage.cleanupArchiveStages(); storage.cleanupUndoCache()
            runCatching { IncomingMedia.cleanup(this@FrameOutApp, container.projects.allSourceUris(), container.prefs.lastVideoUri()) }
            runCatching { IncomingMedia.releaseStaleGrants(this@FrameOutApp, container.projects.allSourceUris(), container.prefs.lastVideoUri()) }
        }
    }
}
