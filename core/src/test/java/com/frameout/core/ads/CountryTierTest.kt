package com.frameout.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class CountryTierTest {
    @Test
    fun mapsKnownCountries() {
        assertEquals(CountryTier.High, CountryTiers.of("us"))
        assertEquals(CountryTier.Mid, CountryTiers.of("SA"))
        assertEquals(CountryTier.Low, CountryTiers.of("PS"))
    }

    @Test
    fun unknownOrBlankIsLow() {
        assertEquals(CountryTier.Low, CountryTiers.of(null))
        assertEquals(CountryTier.Low, CountryTiers.of(""))
    }
}
