package com.frameout.app.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.BuildConfig
import com.frameout.app.FrameOutApp
import com.frameout.core.ads.AdStrings
import com.frameout.core.pro.ProFeature
import com.frameout.designsystem.components.DialogTone
import com.frameout.designsystem.components.FrameConfirmDialog
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
private fun rememberHub(): AdsHub = (LocalContext.current.applicationContext as FrameOutApp).ads

@Composable
fun AdBannerSlot() {
    val hub = rememberHub()
    val state by hub.bannerState.collectAsState()
    if (state == BannerState.Hidden) return
    val widthDp = LocalConfiguration.current.screenWidthDp
    val reserved = remember(widthDp) { hub.bannerHeightDp(widthDp) }
    Box(Modifier.fillMaxWidth().heightIn(min = reserved.dp)) {
        AdBannerView(Modifier.fillMaxWidth(), BuildConfig.ADMOB_BANNER_ID, hub::onBannerResult)
    }
}

@Composable
fun AdsHost() {
    val app = LocalContext.current.applicationContext as FrameOutApp
    val hub = app.ads
    val blocked by hub.blockedNotice.collectAsState()
    if (!blocked) return
    FrameConfirmDialog(
        title = AdStrings.blockedTitle(),
        body = AdStrings.blockedBody(),
        confirmLabel = AdStrings.rewardPro(),
        dismissLabel = AdStrings.blockedClose(),
        onConfirm = {
            hub.dismissBlockedNotice()
            app.container.entitlements.showPaywall(ProFeature.General)
        },
        onDismiss = hub::dismissBlockedNotice,
        tone = DialogTone.Info,
        icon = FrameIconsExtra.Warning
    )
}

@Composable
fun RewardOffer(feature: ProFeature, onPro: () -> Unit, onFinished: () -> Unit) {
    val hub = rememberHub()
    FrameConfirmDialog(
        title = AdStrings.rewardTitle(),
        body = AdStrings.rewardBody(),
        confirmLabel = AdStrings.rewardWatch(),
        dismissLabel = AdStrings.rewardPro(),
        onConfirm = { hub.showReward(feature) { earned -> if (earned) onFinished() else onPro() } },
        onDismiss = onPro,
        tone = DialogTone.Info
    )
}

@Composable
fun rewardOfferAvailable(feature: ProFeature): Boolean {
    val hub = rememberHub()
    val ready by hub.rewardReady.collectAsState()
    return ready && feature != ProFeature.General && hub.rewardAvailable()
}
