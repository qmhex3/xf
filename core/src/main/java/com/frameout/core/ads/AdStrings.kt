package com.frameout.core.ads

import com.frameout.core.R
import com.frameout.core.util.StringSource

object AdStrings {
    private fun s(id: Int): String = StringSource.get(id)

    fun rewardTitle() = s(R.string.ads_reward_title)
    fun rewardBody() = s(R.string.ads_reward_body)
    fun rewardWatch() = s(R.string.ads_reward_watch)
    fun rewardPro() = s(R.string.ads_reward_pro)
    fun blockedTitle() = s(R.string.ads_blocked_title)
    fun blockedBody() = s(R.string.ads_blocked_body)
    fun blockedClose() = s(R.string.ads_blocked_close)
    fun privacyOptions() = s(R.string.ads_privacy_options)
}
