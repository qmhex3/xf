package com.frameout.engine.work

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.app.NotificationCompat
import com.frameout.core.db.AppDatabase
import com.frameout.core.util.FrameStrings
import com.frameout.engine.R

internal object ExtractDoneNotification {
    const val CHANNEL_ID = "frameout_extraction_done_v2"
    private const val LEGACY_CHANNEL_ID = "frameout_extraction_done"
    private const val ID_OFFSET = 0x800000
    private const val SAMPLES = 4
    private const val COLLAGE_W = 1000
    private const val COLLAGE_H = 360
    private const val GAP = 8

    suspend fun show(context: Context, projectId: Long) {
        if (appVisible()) return
        val db = AppDatabase.get(context)
        val project = db.projectDao().getByIdSync(projectId) ?: return
        val count = db.frameDao().countActive(projectId)
        if (count <= 0) return
        val paths = (0 until SAMPLES).mapNotNull { i ->
            db.frameDao().pageActive(projectId, 1, (count - 1) * i / (SAMPLES - 1)).firstOrNull()?.thumbnailPath
        }
        val open = openIntent(context, projectId)
        val title = FrameStrings.extractDoneTitle("%,d".format(count))
        val builder = NotificationCompat.Builder(context, channel(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(title)
            .setContentText(project.name.ifBlank { FrameStrings.appName() })
            .setContentIntent(open)
            .setSound(chimeUri(context))
            .setAutoCancel(true)
            .addAction(0, FrameStrings.openFrames(), open)
        collage(paths)?.let { builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(it).setSummaryText(project.name)) }
        context.getSystemService(NotificationManager::class.java).notify(ExtractNotifications.notificationId(projectId) + ID_OFFSET, builder.build())
    }

    fun cancel(context: Context, projectId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(ExtractNotifications.notificationId(projectId) + ID_OFFSET)
    }

    private fun appVisible(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun chimeUri(context: Context): android.net.Uri =
        android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.extract_done_chime}")

    private fun channel(context: Context): String {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            val channel = NotificationChannel(CHANNEL_ID, FrameStrings.extractDoneChannel(), NotificationManager.IMPORTANCE_DEFAULT)
            channel.setSound(
                chimeUri(context),
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            manager.createNotificationChannel(channel)
        }
        return CHANNEL_ID
    }

    private fun openIntent(context: Context, projectId: Long): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(ExtractNotifications.EXTRA_OPEN_PROJECT, projectId)
        intent.putExtra(ExtractNotifications.EXTRA_OPEN_TOKEN, com.frameout.core.util.IntentToken.get(context))
        val code = ExtractNotifications.notificationId(projectId) + ID_OFFSET
        return PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun collage(paths: List<String?>): Bitmap? {
        val decoded = paths.mapNotNull { p -> p?.let { BitmapFactory.decodeFile(it) } }
        if (decoded.isEmpty()) return null
        val out = Bitmap.createBitmap(COLLAGE_W, COLLAGE_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val cell = (COLLAGE_W - GAP * (decoded.size - 1)) / decoded.size
        decoded.forEachIndexed { i, bmp ->
            val left = i * (cell + GAP)
            canvas.drawBitmap(bmp, centerCrop(bmp, cell, COLLAGE_H), Rect(left, 0, left + cell, COLLAGE_H), null)
            bmp.recycle()
        }
        return out
    }

    private fun centerCrop(bmp: Bitmap, w: Int, h: Int): Rect {
        val target = w.toFloat() / h
        val source = bmp.width.toFloat() / bmp.height
        return if (source > target) {
            val cropW = (bmp.height * target).toInt()
            Rect((bmp.width - cropW) / 2, 0, (bmp.width + cropW) / 2, bmp.height)
        } else {
            val cropH = (bmp.width / target).toInt()
            Rect(0, (bmp.height - cropH) / 2, bmp.width, (bmp.height + cropH) / 2)
        }
    }
}
