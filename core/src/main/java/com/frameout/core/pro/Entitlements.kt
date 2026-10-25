package com.frameout.core.pro

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Entitlements(context: Context, val enforced: Boolean, legacyInstall: Boolean) {
    private val prefs: SharedPreferences = runCatching { buildEncryptedPrefs(context) }
        .getOrElse { context.applicationContext.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE) }
    private val tickets = HashMap<ProFeature, Long>()
    private val triedItems = HashSet<String>()

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _paywall = MutableStateFlow<ProFeature?>(null)
    val paywall: StateFlow<ProFeature?> = _paywall.asStateFlow()

    init {
        if (!prefs.getBoolean(KEY_SEEDED, false)) {
            prefs.edit().putBoolean(KEY_SEEDED, true).putBoolean(KEY_LEGACY, legacyInstall).apply()
        }
        _isPro.value = computePro(System.currentTimeMillis())
        ProAccess.source = this
    }

    val legacy: Boolean get() = prefs.getBoolean(KEY_LEGACY, false)

    private fun computePro(now: Long): Boolean {
        if (!enforced || legacy) return true
        val verified = prefs.getLong(KEY_VERIFIED, 0L)
        val fresh = now - verified < ProCatalog.GRACE_DAYS * DAY_MS
        return prefs.getBoolean(KEY_ACTIVE, false) && fresh
    }

    fun applyBillingResult(active: Boolean, plan: String) {
        prefs.edit().putBoolean(KEY_ACTIVE, active).putString(KEY_PLAN, if (active) plan else "")
            .putLong(KEY_VERIFIED, System.currentTimeMillis()).apply()
        _isPro.value = computePro(System.currentTimeMillis())
    }

    val plan: String get() = prefs.getString(KEY_PLAN, "") ?: ""

    fun permits(feature: ProFeature): Boolean {
        if (_isPro.value) return true
        val until = synchronized(tickets) { tickets[feature] } ?: return false
        return System.currentTimeMillis() < until
    }

    fun trialsLeft(feature: ProFeature): Int =
        (ProCatalog.TRIALS_PER_FEATURE - prefs.getInt(trialKey(feature), 0)).coerceAtLeast(0)

    fun tryOnce(feature: ProFeature, item: String = ""): Boolean {
        if (_isPro.value) return true
        val tag = feature.key + ":" + item
        if (item.isNotEmpty() && tag in triedItems) return true
        if (trialsLeft(feature) <= 0) return false
        prefs.edit().putInt(trialKey(feature), prefs.getInt(trialKey(feature), 0) + 1).apply()
        if (item.isNotEmpty()) triedItems.add(tag)
        synchronized(tickets) { tickets[feature] = System.currentTimeMillis() + TICKET_MS }
        return true
    }

    fun grantTicket(feature: ProFeature, durationMs: Long) {
        synchronized(tickets) { tickets[feature] = System.currentTimeMillis() + durationMs }
    }

    fun gate(feature: ProFeature, free: Boolean, item: String, action: () -> Unit) {
        if (!enforced || free || tryOnce(feature, item)) action() else showPaywall(feature)
    }

    fun showPaywall(feature: ProFeature) { _paywall.value = feature }

    fun dismissPaywall() { _paywall.value = null }

    private fun trialKey(feature: ProFeature) = "trial_" + feature.key

    companion object {
        private const val FILE = "frameout_pro_enc"
        private const val FALLBACK_FILE = "frameout_pro_enc_fallback"
        private const val KEY_SEEDED = "seeded"
        private const val KEY_LEGACY = "legacy"
        private const val KEY_ACTIVE = "active"
        private const val KEY_PLAN = "plan"
        private const val KEY_VERIFIED = "verified_at"
        private const val DAY_MS = 86_400_000L
        private const val TICKET_MS = 10 * 60_000L

        private fun buildEncryptedPrefs(context: Context): SharedPreferences {
            val app = context.applicationContext
            val masterKey = MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                app,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}

object ProAccess {
    @Volatile var source: Entitlements? = null

    fun permits(feature: ProFeature): Boolean = source?.permits(feature) ?: false

    fun isProActive(): Boolean = source?.isPro?.value ?: false

    fun formatAllowed(format: com.frameout.core.storage.ImageFormat): Boolean =
        ProCatalog.formatIsFree(format) || permits(ProFeature.ExportFormats)

    fun applyVerifiedEntitlement(active: Boolean, plan: String) {
        source?.applyBillingResult(active, plan)
    }
}
