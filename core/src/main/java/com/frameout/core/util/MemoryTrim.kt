package com.frameout.core.util

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import java.util.concurrent.atomic.AtomicBoolean

object MemoryTrim : ComponentCallbacks2 {
    private const val LOW_MEMORY_LEVEL = 80
    private val registered = AtomicBoolean(false)

    fun register(context: Context) {
        if (registered.compareAndSet(false, true)) context.applicationContext.registerComponentCallbacks(this)
    }

    override fun onTrimMemory(level: Int) {
        ThumbCache.trim(level)
    }

    override fun onLowMemory() {
        ThumbCache.trim(LOW_MEMORY_LEVEL)
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit
}
