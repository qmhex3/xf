package com.frameout.app.billing

import android.app.Activity
import com.frameout.core.pro.Entitlements
import com.frameout.core.util.DiagLog
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BillingNote { None, Unavailable, Error, Pending, Restored, NothingToRestore }

class BillingManager(
    private val entitlements: Entitlements,
    private val scope: CoroutineScope
) {
    private val _offers = MutableStateFlow<List<PlanOffer>>(emptyList())
    val offers: StateFlow<List<PlanOffer>> = _offers.asStateFlow()

    private val _note = MutableStateFlow(BillingNote.None)
    val note: StateFlow<BillingNote> = _note.asStateFlow()

    fun start() {
        if (!entitlements.enforced) return
        if (!RevenueCatSync.isActive) {
            _note.value = BillingNote.Unavailable
            return
        }
        scope.launch { refresh() }
    }

    suspend fun refresh() {
        _offers.value = BillingCatalog.offersOf(RevenueCatSync.currentOffering())
        RevenueCatSync.refresh()
    }

    fun restore() {
        if (!RevenueCatSync.isActive) {
            _note.value = BillingNote.Unavailable
            return
        }
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error ->
                DiagLog.append("billing", "RevenueCat restore failed: ${error.code}")
                _note.value = BillingNote.Error
            },
            onSuccess = { info ->
                RevenueCatSync.apply(info)
                _note.value = if (RevenueCatSync.isEntitled(info)) BillingNote.Restored else BillingNote.NothingToRestore
            }
        )
    }

    fun launch(activity: Activity, offer: PlanOffer) {
        if (!RevenueCatSync.isActive) {
            _note.value = BillingNote.Unavailable
            return
        }
        scope.launch(Dispatchers.Main.immediate) {
            try {
                val result = Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, offer.rcPackage).build())
                RevenueCatSync.apply(result.customerInfo)
            } catch (e: PurchasesTransactionException) {
                when {
                    e.userCancelled -> Unit
                    e.code == PurchasesErrorCode.PaymentPendingError -> _note.value = BillingNote.Pending
                    else -> {
                        DiagLog.append("billing", "RevenueCat purchase failed: ${e.code} ${e.underlyingErrorMessage}")
                        _note.value = BillingNote.Error
                    }
                }
            }
        }
    }

    fun clearNote() { _note.value = BillingNote.None }
}
