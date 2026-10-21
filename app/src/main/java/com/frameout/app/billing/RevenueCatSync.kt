package com.frameout.app.billing

import android.content.Context
import com.frameout.app.BuildConfig
import com.frameout.core.pro.ProAccess
import com.frameout.core.util.DiagLog
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.CancellationException

object RevenueCatSync {
    private const val TEST_STORE_PREFIX = "test_"
    private const val MAX_INFO_AGE_MS = 2 * 86_400_000L

    private val entitlementId get() = BuildConfig.REVENUECAT_ENTITLEMENT_ID

    val isActive: Boolean get() = Purchases.isConfigured

    fun configure(context: Context) {
        val key = BuildConfig.REVENUECAT_API_KEY
        if (key.isBlank()) {
            DiagLog.append("billing", "RevenueCat not configured: missing API key")
            return
        }
        if (!BuildConfig.DEBUG && key.startsWith(TEST_STORE_PREFIX)) {
            DiagLog.append("billing", "RevenueCat not configured: Test Store key in a release build")
            return
        }
        if (Purchases.isConfigured) return
        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.ERROR
        Purchases.configure(
            PurchasesConfiguration.Builder(context.applicationContext, key)
                .purchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
                .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
                .build()
        )
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { apply(it) }
    }

    suspend fun refresh(): CustomerInfo? {
        if (!isActive) return null
        return try {
            Purchases.sharedInstance.awaitCustomerInfo().also(::apply)
        } catch (e: CancellationException) {
            throw e
        } catch (e: PurchasesException) {
            DiagLog.append("billing", "RevenueCat customer info failed: ${e.error.message}")
            null
        }
    }

    suspend fun currentOffering(): Offering? {
        if (!isActive) return null
        return try {
            Purchases.sharedInstance.awaitOfferings().current
        } catch (e: CancellationException) {
            throw e
        } catch (e: PurchasesException) {
            DiagLog.append("billing", "RevenueCat offerings failed: ${e.error.message}")
            null
        }
    }

    fun isEntitled(info: CustomerInfo): Boolean {
        val forged = !BuildConfig.DEBUG && info.entitlements.verification == VerificationResult.FAILED
        return !forged && info.entitlements[entitlementId]?.isActive == true
    }

    fun apply(info: CustomerInfo) {
        if (System.currentTimeMillis() - info.requestDate.time > MAX_INFO_AGE_MS) return
        val entitled = isEntitled(info)
        val plan = when {
            !entitled -> ""
            info.entitlements[entitlementId]?.expirationDate == null -> "lifetime"
            else -> "sub"
        }
        ProAccess.applyVerifiedEntitlement(entitled, plan)
    }
}
