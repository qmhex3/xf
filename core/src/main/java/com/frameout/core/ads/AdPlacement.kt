package com.frameout.core.ads

enum class AdPlacement { ExportDone, ScreenBreak }

enum class DenyReason {
    Inactive, NoConsent, Grace, Warmup, TooSoon, NotEnoughActions,
    SessionCap, DailyCap, AfterReward, ClockBack
}

sealed interface AdVerdict {
    data object Allow : AdVerdict
    data class Deny(val reason: DenyReason) : AdVerdict
}

enum class AdState { Unknown, Healthy, NoFill, Blocked }
