package com.frameout.engine.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.frameout.core.util.FrameStrings
import com.frameout.core.db.AppDatabase
import com.frameout.engine.extract.ExtractProgress

object ExtractNotifications {
    const val CHANNEL_ID = "frameout_extraction"
    const val NOTIFICATION_ID = 4107
    internal const val BRAND_COLOR = 0xFF7A4CF6.toInt()
    const val EXTRA_OPEN_PROJECT = "open_project_id"
    const val EXTRA_OPEN_TOKEN = "open_token"
    const val TRANSCODE_ID = 4106
    private const val QUALITY_ID_OFFSET = 0x200000
    private const val BATCH_ID_BASE = 0x400000
    private const val RETRY_ID_OFFSET = 0x600000
    private const val BLOCKED_ID_OFFSET = 0x800000
    const val EXTRA_PROJECT_ID = "project_id"
    const val ACTION_PAUSE = "com.frameout.EXTRACT_PAUSE"
    const val ACTION_RESUME = "com.frameout.EXTRACT_RESUME"
    const val ACTION_CANCEL = "com.frameout.EXTRACT_CANCEL"

    fun notificationId(projectId: Long): Int = NOTIFICATION_ID + ((projectId xor (projectId ushr 32)).toInt() and 0x1FFFFF)

    fun build(context: Context, projectId: Long, text: String, done: Int, total: Int, indeterminate: Boolean, paused: Boolean, elapsed: Long, remaining: Long): Notification {
        channel(context)
        return ExtractProgressNotification.build(context, projectId, text, done, total, indeterminate, paused, elapsed, remaining)
    }

    private const val COPY_BLOCKED_ID = 4105

    fun showCopyBlocked(context: Context) {
        channel(context)
        val text = FrameStrings.foregroundBlocked()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.frameout.engine.R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(FrameStrings.appName())
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(launchIntent(context))
            .setOnlyAlertOnce(true)
            .build()
        runCatching { context.getSystemService(NotificationManager::class.java).notify(COPY_BLOCKED_ID, notification) }
    }

    fun clearCopyBlocked(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(COPY_BLOCKED_ID)
    }

    fun buildTranscoding(context: Context): Notification {
        channel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.frameout.engine.R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(FrameStrings.appName())
            .setContentText(FrameStrings.working())
            .setContentIntent(launchIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true)
            .build()
    }

    fun showCompletionNotices(context: Context, projectId: Long, message: String) {
        showLowSpaceIf(context, projectId, message)
        showReducedQuality(context, projectId, com.frameout.engine.decode.DecodeQuality.takeReduction())
    }

    fun showLowSpaceIf(context: Context, projectId: Long, message: String) {
        if (message == ExtractProgress.MESSAGE_LOW_SPACE) showPaused(context, projectId, FrameStrings.lowStorageResume())
    }

    fun showBatchComplete(context: Context, batchName: String, completed: Int, total: Int) {
        channel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.frameout.engine.R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(FrameStrings.batchQueue())
            .setContentText(FrameStrings.finished(completed, total))
            .setContentIntent(launchIntent(context))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(BATCH_ID_BASE + (batchName.hashCode() and 0xFFFF), notification)
    }

    fun showRetrying(context: Context, projectId: Long) {
        channel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.frameout.engine.R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(projectTitle(context, projectId))
            .setContentText(FrameStrings.retryingSoon())
            .setContentIntent(launchIntent(context))
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .addAction(0, FrameStrings.cancel(), actionIntent(context, ACTION_CANCEL, projectId))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notificationId(projectId) + RETRY_ID_OFFSET, notification)
    }

    fun showForegroundBlocked(context: Context, projectId: Long) {
        channel(context)
        val text = FrameStrings.foregroundBlocked()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.frameout.engine.R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(projectTitle(context, projectId))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(launchIntent(context))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notificationId(projectId) + BLOCKED_ID_OFFSET, notification)
    }

    fun clearRetrying(context: Context, projectId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(projectId) + RETRY_ID_OFFSET)
    }

    fun showReducedQuality(context: Context, projectId: Long, reduction: com.frameout.engine.decode.DecodeQuality.Reduction?) {
        if (reduction == null) return
        channel(context)
        val text = if (reduction.sourceWidth > 0 && reduction.outputWidth > 0) {
            FrameStrings.reducedQualityTo(reduction.count, reduction.sourceWidth, reduction.sourceHeight, reduction.outputWidth, reduction.outputHeight)
        } else {
            FrameStrings.reducedQuality(reduction.count)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.frameout.engine.R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(projectTitle(context, projectId))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notificationId(projectId) + QUALITY_ID_OFFSET, notification)
    }

    fun showPaused(context: Context, projectId: Long, text: String = FrameStrings.paused()) {
        channel(context)
        val intent = Intent(ACTION_RESUME).setPackage(context.packageName).putExtra(EXTRA_PROJECT_ID, projectId)
        val open = PendingIntent.getBroadcast(context, requestCode(ACTION_RESUME, projectId), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.frameout.engine.R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(projectTitle(context, projectId))
            .setContentText(text)
            .setContentIntent(launchIntent(context))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .addAction(0, FrameStrings.resume(), open)
            .addAction(0, FrameStrings.cancel(), actionIntent(context, ACTION_CANCEL, projectId))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notificationId(projectId), notification)
    }

    internal fun launchIntent(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun cancel(context: Context, projectId: Long) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(notificationId(projectId))
        ExtractStrip.forget(projectId)
        ExtractDoneNotification.cancel(context, projectId)
        manager.cancel(notificationId(projectId) + RETRY_ID_OFFSET)
        manager.cancel(notificationId(projectId) + BLOCKED_ID_OFFSET)
    }

    private fun channel(context: Context) {
        if (channelReady) return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, FrameStrings.extraction(), NotificationManager.IMPORTANCE_LOW))
        }
        channelReady = true
    }

    internal fun actionIntent(context: Context, action: String, projectId: Long): PendingIntent {
        val intent = Intent(action).setPackage(context.packageName).putExtra(EXTRA_PROJECT_ID, projectId)
        return PendingIntent.getBroadcast(context, requestCode(action, projectId), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun requestCode(action: String, projectId: Long): Int = action.hashCode() xor ((projectId xor (projectId ushr 32)).toInt())

    internal fun formatTime(ms: Long): String = when {
        ms >= 3_600_000L -> "%dh %02dm".format(ms / 3_600_000L, (ms / 60_000L) % 60)
        ms >= 60_000L -> "%dm %02ds".format(ms / 60_000L, (ms / 1000L) % 60)
        else -> "%ds".format(ms / 1000L)
    }

    @Volatile private var channelReady = false

    private val titleCache = java.util.concurrent.ConcurrentHashMap<Long, String>()

    fun invalidateTitle(projectId: Long) {
        titleCache.remove(projectId)
    }

    internal fun thumbnailOf(context: Context, projectId: Long): android.graphics.Bitmap? {
        val path = try {
            AppDatabase.get(context).projectDao().getByIdSync(projectId)?.thumbnailPath
        } catch (_: Exception) {
            null
        }
        return ExtractStrip.thumb(projectId, path)
    }

    internal fun projectTitle(context: Context, projectId: Long): String {
        titleCache[projectId]?.let { return it }
        val fallback = "${FrameStrings.appName()} #$projectId"
        return try {
            val db = AppDatabase.get(context)
            val name = db.projectDao().getByIdSync(projectId)?.name
            val title = if (!name.isNullOrBlank()) name else fallback
            titleCache[projectId] = title
            title
        } catch (_: Exception) {
            fallback
        }
    }
}

