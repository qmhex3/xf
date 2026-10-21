package com.frameout.engine.work

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.frameout.core.util.logFailure
import java.util.concurrent.atomic.AtomicInteger

object ReencodeForeground {
    private val holders = AtomicInteger(0)
    private val main = Handler(Looper.getMainLooper())

    suspend fun <T> around(context: Context, block: suspend () -> T): T {
        acquire(context.applicationContext)
        try {
            return block()
        } finally {
            release(context.applicationContext)
        }
    }

    private fun acquire(context: Context) {
        if (holders.getAndIncrement() != 0) return
        runCatching { ContextCompat.startForegroundService(context, Intent(context, TranscodeService::class.java)) }
            .onFailure { logFailure("ReencodeForeground", it) }
    }

    private fun release(context: Context) {
        if (holders.decrementAndGet() > 0) return
        holders.set(0)
        main.postDelayed({
            if (holders.get() == 0) runCatching { context.stopService(Intent(context, TranscodeService::class.java)) }
        }, STOP_DELAY_MS)
    }

    private const val STOP_DELAY_MS = 1_500L
}
