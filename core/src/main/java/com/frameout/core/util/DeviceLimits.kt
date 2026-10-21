package com.frameout.core.util

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object DeviceLimits {
    private const val DEFAULT_DECODES = 2
    private const val DEFAULT_THUMB_DECODES = 4
    private const val DEFAULT_POOL_BYTES = 32L * 1024L * 1024L

    @Volatile private var device: DeviceCapability? = null

    fun install(capability: DeviceCapability) {
        device = capability
    }

    fun decodes(): Int = runCatching { device?.maxConcurrentDecodes }.getOrNull() ?: DEFAULT_DECODES

    fun thumbDecodes(): Int = runCatching { device?.maxThumbDecodes }.getOrNull() ?: DEFAULT_THUMB_DECODES

    fun poolBytes(): Long = runCatching { device?.poolBytes }.getOrNull() ?: DEFAULT_POOL_BYTES

    fun analysisReduced(): Boolean = runCatching { device?.analysisReduced }.getOrNull() ?: false

    fun scanCooldownMs(): Long = runCatching { device?.scanCooldownMs }.getOrNull() ?: 0L

    fun preferLiteGlass(): Boolean = runCatching { device?.preferLiteGlass }.getOrNull() ?: false
}

class AdaptiveGate(private val max: Int, private val limit: () -> Int) {
    private val permits = Semaphore(max)
    private val tiers = Array(max) { Semaphore(it + 1) }

    suspend fun <T> withPermit(block: suspend () -> T): T = permits.withPermit {
        tiers[limit().coerceIn(1, max) - 1].withPermit { block() }
    }
}
