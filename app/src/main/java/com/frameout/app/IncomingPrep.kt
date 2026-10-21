package com.frameout.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object IncomingPrep {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var running = 0
    private val jobs = mutableSetOf<Job>()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _result = MutableStateFlow<IncomingMedia.Stabilized?>(null)
    val result: StateFlow<IncomingMedia.Stabilized?> = _result

    fun start(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = context.applicationContext
        synchronized(lock) { running++; _busy.value = true }
        runCatching { androidx.core.content.ContextCompat.startForegroundService(app, android.content.Intent(app, IncomingCopyService::class.java)) }
        val job = scope.launch {
            val done: IncomingMedia.Stabilized? = try {
                IncomingMedia.stabilize(app, uris)
            } catch (cancelled: CancellationException) {
                null
            } catch (e: Exception) {
                IncomingMedia.Stabilized(emptyList(), uris.size)
            }
            val result = done ?: return@launch
            synchronized(lock) {
                val previous = _result.value
                _result.value = if (previous == null) result
                else IncomingMedia.Stabilized(previous.ready + result.ready, previous.failed + result.failed, minOf(previous.at, result.at), previous.rejected + result.rejected)
            }
        }
        synchronized(lock) { jobs += job }
        job.invokeOnCompletion {
            synchronized(lock) {
                jobs -= job
                running--
                _busy.value = running > 0
                if (running == 0) com.frameout.engine.work.ExtractNotifications.clearCopyBlocked(app)
            }
        }
    }

    fun cancel() {
        val active = synchronized(lock) { jobs.toList() }
        active.forEach { it.cancel() }
    }

    fun take(): IncomingMedia.Stabilized? = synchronized(lock) {
        val pending = _result.value
        _result.value = null
        pending?.takeIf { System.currentTimeMillis() - it.at <= STALE_AFTER_MS }
    }

    private const val STALE_AFTER_MS = 30L * 60L * 1000L
}
