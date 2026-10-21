package com.frameout.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.frameout.engine.work.ExtractNotifications
import com.frameout.engine.work.ExtractScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ExtractActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val projectId = intent.getLongExtra(ExtractNotifications.EXTRA_PROJECT_ID, -1L)
        if (projectId < 0) return
        val appContext = context.applicationContext
        val work = when (action) {
            ExtractNotifications.ACTION_PAUSE -> ExtractScheduler.pause(appContext, projectId)
            ExtractNotifications.ACTION_RESUME -> ExtractScheduler.resume(appContext, projectId)
            ExtractNotifications.ACTION_CANCEL -> ExtractScheduler.cancel(appContext, projectId)
            else -> return
        }
        finishWhenDone(goAsync(), work)
    }

    private fun finishWhenDone(pending: PendingResult, work: Job) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                withTimeoutOrNull(WAIT_MS) { work.join() }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val WAIT_MS = 8_000L
    }
}
