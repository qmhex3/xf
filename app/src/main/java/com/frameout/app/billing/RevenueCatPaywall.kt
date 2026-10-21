package com.frameout.app.billing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme as materialDarkScheme
import androidx.compose.material3.lightColorScheme as materialLightScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frameout.designsystem.theme.FrameTheme
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter

private val FullScreen = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)

@Composable
internal fun RevenueCatPaywall(onDismiss: () -> Unit) {
    val listener = remember {
        object : PaywallListener {
            override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) = RevenueCatSync.apply(customerInfo)
            override fun onRestoreCompleted(customerInfo: CustomerInfo) = RevenueCatSync.apply(customerInfo)
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = FullScreen) {
        Box(Modifier.fillMaxSize()) {
            Paywall(
                PaywallOptions.Builder(dismissRequest = onDismiss)
                    .setListener(listener)
                    .setShouldDisplayDismissButton(true)
                    .build()
            )
        }
    }
}

@Composable
fun CustomerCenterHost(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = FullScreen) {
        MaterialTheme(colorScheme = frameMaterialScheme()) {
            CustomerCenter(modifier = Modifier.fillMaxSize(), onDismiss = onDismiss)
        }
    }
}

@Composable
private fun frameMaterialScheme(): ColorScheme {
    val c = FrameTheme.colors
    return if (c.isDark) {
        materialDarkScheme(
            primary = c.primary, onPrimary = c.onPrimary, primaryContainer = c.primaryContainer,
            background = c.background, onBackground = c.onSurface, surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.container, onSurfaceVariant = c.onSurfaceVariant, outline = c.outline, error = c.error
        )
    } else {
        materialLightScheme(
            primary = c.primary, onPrimary = c.onPrimary, primaryContainer = c.primaryContainer,
            background = c.background, onBackground = c.onSurface, surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.container, onSurfaceVariant = c.onSurfaceVariant, outline = c.outline, error = c.error
        )
    }
}
