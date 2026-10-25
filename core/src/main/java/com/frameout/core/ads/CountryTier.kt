package com.frameout.core.ads

enum class CountryTier { High, Mid, Low }

object CountryTiers {
    private val high = setOf(
        "US", "CA", "GB", "AU", "NZ", "IE", "DE", "FR", "NL", "SE", "NO", "DK", "FI",
        "CH", "AT", "BE", "LU", "JP", "KR", "SG", "IS"
    )
    private val mid = setOf(
        "AE", "SA", "QA", "KW", "BH", "OM", "IL", "ES", "IT", "PT", "PL", "CZ", "HU", "GR",
        "CY", "MT", "SK", "SI", "EE", "LV", "LT", "HR", "RO", "BG", "HK", "TW", "MY", "TH",
        "CL", "UY", "TR", "MX", "BR", "AR", "ZA", "RU", "UA", "KZ"
    )

    fun of(iso: String?): CountryTier {
        val code = iso?.trim()?.uppercase().orEmpty()
        return when {
            code in high -> CountryTier.High
            code in mid -> CountryTier.Mid
            else -> CountryTier.Low
        }
    }
}
