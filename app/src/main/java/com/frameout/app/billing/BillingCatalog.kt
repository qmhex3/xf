package com.frameout.app.billing

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import kotlin.math.roundToInt

enum class ProPlan(val productId: String, val basePlanId: String?) {
    Monthly("frameout_pro", "monthly"),
    Yearly("frameout_pro", "yearly"),
    Lifetime("frameout_pro_lifetime", null)
}

data class PlanOffer(
    val plan: ProPlan,
    val rcPackage: Package,
    val price: String,
    val freeTrial: Boolean,
    val micros: Long = 0L,
    val trialDays: Int = 0
)

object BillingCatalog {
    const val SUB_ID = "frameout_pro"
    const val LIFETIME_ID = "frameout_pro_lifetime"

    fun offersOf(offering: Offering?): List<PlanOffer> =
        offering?.availablePackages.orEmpty().mapNotNull(::offerOf).distinctBy { it.plan }

    private fun offerOf(pkg: Package): PlanOffer? {
        val product = pkg.product
        val plan = planFor(pkg.packageType, product.id) ?: return null
        val trial = product.subscriptionOptions?.freeTrial
        val days = trial?.freePhase?.let { trialDaysOf(it.billingPeriod.iso8601) } ?: 0
        return PlanOffer(plan, pkg, product.price.formatted, trial != null, product.price.amountMicros, days)
    }

    internal fun planFor(type: PackageType, productId: String): ProPlan? = when (type) {
        PackageType.MONTHLY -> ProPlan.Monthly
        PackageType.ANNUAL -> ProPlan.Yearly
        PackageType.LIFETIME -> ProPlan.Lifetime
        else -> {
            val id = productId.lowercase()
            when {
                "life" in id -> ProPlan.Lifetime
                "year" in id || "annual" in id -> ProPlan.Yearly
                "month" in id -> ProPlan.Monthly
                else -> null
            }
        }
    }

    internal fun trialDaysOf(period: String): Int {
        val match = Regex("P(?:(\\d+)W)?(?:(\\d+)D)?").matchEntire(period) ?: return 0
        return (match.groupValues[1].toIntOrNull() ?: 0) * 7 + (match.groupValues[2].toIntOrNull() ?: 0)
    }

    internal fun savingsPercent(monthlyMicros: Long, yearlyMicros: Long): Int {
        if (monthlyMicros <= 0L || yearlyMicros <= 0L) return 0
        return ((1.0 - yearlyMicros.toDouble() / (monthlyMicros * 12.0)) * 100.0).roundToInt().coerceAtLeast(0)
    }
}
