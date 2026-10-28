package com.frameout.app.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class RemoteWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val m = RemoteSync.sync(ctx) ?: return Result.success()
        val store = RemoteStore(ctx)
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return Result.success()
        m.notices.filter { it.isActive() && !store.shown(it.id) }.forEach { show(it); store.markShown(it.id) }
        return Result.success()
    }

    private fun show(n: Notice) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "Announcements", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = n.link?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)) }
            ?: ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val pi = PendingIntent.getActivity(ctx, n.id.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = NotificationCompat.Builder(ctx, CH).setSmallIcon(ctx.applicationInfo.icon)
            .setContentTitle(n.title).setContentText(n.body).setAutoCancel(true).setContentIntent(pi)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body))
        if (n.image != null && n.sha != null) RemoteSync.media(ctx, n.image, n.sha)?.let { f ->
            BitmapFactory.decodeFile(f.path)?.let { b.setLargeIcon(it).setStyle(NotificationCompat.BigPictureStyle().bigPicture(it)) }
        }
        runCatching { NotificationManagerCompat.from(ctx).notify(n.id.hashCode(), b.build()) }
    }

    companion object {
        private const val CH = "remote_notices"
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<RemoteWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("remote_content", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
