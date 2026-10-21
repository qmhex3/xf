package com.frameout.app.billing

import androidx.compose.ui.graphics.Color
import com.revenuecat.purchases.PackageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingCatalogTest {
    @Test
    fun trialLengthIsReadFromTheBillingPeriod() {
        assertEquals(3, BillingCatalog.trialDaysOf("P3D"))
        assertEquals(7, BillingCatalog.trialDaysOf("P1W"))
        assertEquals(10, BillingCatalog.trialDaysOf("P1W3D"))
        assertEquals(0, BillingCatalog.trialDaysOf("P1M"))
        assertEquals(0, BillingCatalog.trialDaysOf(""))
    }

    @Test
    fun yearlySavingIsComparedWithTwelveMonths() {
        assertEquals(58, BillingCatalog.savingsPercent(2_990_000L, 14_990_000L))
        assertEquals(0, BillingCatalog.savingsPercent(0L, 14_990_000L))
        assertEquals(0, BillingCatalog.savingsPercent(1_000_000L, 20_000_000L))
    }

    @Test
    fun packagesMapToPlansByTypeThenByProductId() {
        assertEquals(ProPlan.Monthly, BillingCatalog.planFor(PackageType.MONTHLY, "monthly"))
        assertEquals(ProPlan.Yearly, BillingCatalog.planFor(PackageType.ANNUAL, "yearly"))
        assertEquals(ProPlan.Lifetime, BillingCatalog.planFor(PackageType.LIFETIME, "lifetime"))
        assertEquals(ProPlan.Lifetime, BillingCatalog.planFor(PackageType.CUSTOM, "frameout_pro_lifetime"))
        assertEquals(ProPlan.Yearly, BillingCatalog.planFor(PackageType.CUSTOM, "frameout_pro:yearly"))
        assertEquals(ProPlan.Monthly, BillingCatalog.planFor(PackageType.UNKNOWN, "frameout_pro:monthly"))
        assertEquals(null, BillingCatalog.planFor(PackageType.WEEKLY, "weekly"))
    }

    @Test
    fun sweepColorsLoopSeamlessly() {
        val palette = listOf(Color.Red, Color.Green, Color.Blue)
        listOf(0f, 0.25f, 0.9f).forEach { phase ->
            val colors = cycleColors(palette, phase)
            assertEquals(13, colors.size)
            assertEquals(colors.first(), colors.last())
        }
        assertTrue(cycleColors(palette, 0f).first() == Color.Red)
    }
}
