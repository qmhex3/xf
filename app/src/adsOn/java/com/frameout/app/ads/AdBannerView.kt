package com.frameout.app.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdBannerView(modifier: Modifier, unitId: String, onResult: (AdLoad) -> Unit) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val latest = rememberUpdatedState(onResult)
    val view = remember(unitId, widthDp) {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            adUnitId = unitId
            adListener = object : AdListener() {
                override fun onAdLoaded() = latest.value(AdLoad(true))
                override fun onAdFailedToLoad(error: LoadAdError) = latest.value(AdLoad(false, error.code))
            }
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(view) { onDispose { view.destroy() } }
    AndroidView(factory = { view }, modifier = modifier)
}
