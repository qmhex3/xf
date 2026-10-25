package com.frameout.core.ads

data class AdRules(
    val graceSessions: Int,
    val warmupMs: Long,
    val minGapMs: Long,
    val actionsBetween: Int,
    val maxPerSession: Int,
    val maxPerDay: Int,
    val afterRewardQuietMs: Long,
    val rewardsPerDay: Int,
    val rewardTicketMs: Long
) {
    companion object {
        private const val MIN = 60_000L

        fun forTier(tier: CountryTier): AdRules = when (tier) {
            CountryTier.High -> AdRules(2, 2 * MIN, 3 * MIN, 3, 5, 14, 10 * MIN, 6, 10 * MIN)
            CountryTier.Mid -> AdRules(2, 2 * MIN, 4 * MIN, 3, 4, 10, 10 * MIN, 6, 10 * MIN)
            CountryTier.Low -> AdRules(3, 3 * MIN, 6 * MIN, 4, 2, 5, 10 * MIN, 8, 15 * MIN)
        }
    }
}
