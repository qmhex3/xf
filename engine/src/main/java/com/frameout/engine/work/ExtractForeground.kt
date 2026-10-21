package com.frameout.engine.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo

internal object ExtractForeground {
    fun build(context: Context, projectId: Long, text: String, done: Int, total: Int, indeterminate: Boolean, paused: Boolean, elapsed: Long, remaining: Long): ForegroundInfo {
        val notification = ExtractNotifications.build(context, projectId, text, done, total, indeterminate, paused, elapsed, remaining)
        val id = ExtractNotifications.notificationId(projectId)
        return when {
            Build.VERSION.SDK_INT >= 35 -> ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
            Build.VERSION.SDK_INT >= 29 -> ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else -> ForegroundInfo(id, notification)
        }
    }

    fun clockAfter(delayMs: Long): String =
        java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(System.currentTimeMillis() + delayMs))

    fun denied(context: Context, info: ForegroundInfo, projectId: Long, warned: Boolean): Boolean {
        runCatching { context.getSystemService(android.app.NotificationManager::class.java).notify(info.notificationId, info.notification) }
        if (warned || projectId < 0L) return warned
        ExtractNotifications.showForegroundBlocked(context, projectId)
        return true
    }
}
