package com.frameout.engine.work

import android.app.Notification
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.frameout.core.util.FrameStrings
import com.frameout.engine.R

internal object ExtractProgressNotification {
    private const val SMALL_STRIP_PX = 12
    private const val BIG_STRIP_PX = 28

    private const val MIN_CUSTOM_SDK = Build.VERSION_CODES.O

    class Snapshot(
        val title: String,
        val fraction: Float,
        val percent: String,
        val info: String,
        val line: String,
        val known: Boolean,
        val done: Int,
        val total: Int
    )

    fun build(context: Context, projectId: Long, text: String, done: Int, total: Int, indeterminate: Boolean, paused: Boolean, elapsed: Long, remaining: Long): Notification {
        val snap = snapshot(context, projectId, text, done, total, indeterminate, remaining)
        val builder = NotificationCompat.Builder(context, ExtractNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(snap.title)
            .setContentIntent(ExtractNotifications.launchIntent(context))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (Build.VERSION.SDK_INT >= MIN_CUSTOM_SDK) {
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(small(context, snap))
                .setCustomBigContentView(big(context, projectId, snap))
                .setContentText(snap.line)
        } else {
            standard(context, builder, projectId, snap)
        }
        val toggle = if (paused) ExtractNotifications.ACTION_RESUME to FrameStrings.resume() else ExtractNotifications.ACTION_PAUSE to FrameStrings.pause()
        builder.addAction(0, toggle.second, ExtractNotifications.actionIntent(context, toggle.first, projectId))
        builder.addAction(0, FrameStrings.cancel(), ExtractNotifications.actionIntent(context, ExtractNotifications.ACTION_CANCEL, projectId))
        return builder.build()
    }

    private fun standard(context: Context, builder: NotificationCompat.Builder, projectId: Long, snap: Snapshot) {
        builder.setContentText(snap.info)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (snap.known) {
            builder.setProgress(snap.total, snap.done.coerceIn(0, snap.total), false)
            builder.setSubText(snap.percent)
        } else {
            builder.setProgress(0, 0, true)
        }
        ExtractNotifications.thumbnailOf(context, projectId)?.let { builder.setLargeIcon(it) }
    }

    private fun snapshot(context: Context, projectId: Long, text: String, done: Int, total: Int, indeterminate: Boolean, remaining: Long): Snapshot {
        val known = !indeterminate && total > 0
        val fraction = if (known) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
        val percent = if (known) "${(fraction * 100).toInt()}%" else text
        val count = if (known) "%,d / %,d".format(done, total) else ""
        val left = if (remaining > 0L) FrameStrings.remaining(ExtractNotifications.formatTime(remaining)) else ""
        val info = listOf(count, left).filter { it.isNotEmpty() }.joinToString(FrameStrings.sep()).ifEmpty { text }
        val line = listOf(if (known) percent else "", info).filter { it.isNotEmpty() }.joinToString(FrameStrings.sep())
        return Snapshot(ExtractNotifications.projectTitle(context, projectId), fraction, percent, info, line, known, done, total)
    }

    private fun small(context: Context, snap: Snapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.notif_extract_small).apply {
            setTextViewText(R.id.title, snap.title)
            setTextViewText(R.id.info, snap.line)
            setImageViewBitmap(R.id.strip, ExtractStrip.render(context, snap.fraction, SMALL_STRIP_PX))
        }

    private fun big(context: Context, projectId: Long, snap: Snapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.notif_extract_big).apply {
            setTextViewText(R.id.title, snap.title)
            setTextViewText(R.id.percent, snap.percent)
            setTextViewText(R.id.info, snap.info)
            setImageViewBitmap(R.id.strip, ExtractStrip.render(context, snap.fraction, BIG_STRIP_PX))
            val thumb = ExtractNotifications.thumbnailOf(context, projectId)
            if (thumb != null) setImageViewBitmap(R.id.thumb, thumb) else setViewVisibility(R.id.thumb, android.view.View.GONE)
        }
}
