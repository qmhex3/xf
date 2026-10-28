package com.frameout.app.ads

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun AdBannerView(modifier: Modifier, unitId: String, onResult: (AdLoad) -> Unit) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val banner = remember(unitId, widthDp) { BannerCache.obtain(context, unitId, widthDp, onResult) }
    val holder = remember(banner) { FrameLayout(context) }
    AndroidView(
        factory = {
            (banner.parent as? ViewGroup)?.removeView(banner)
            holder.removeAllViews()
            holder.addView(banner, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL))
            holder
        },
        modifier = modifier
    )
    DisposableEffect(banner, holder) {
        onDispose { if (banner.parent === holder) holder.removeView(banner) }
    }
}
