package com.frameout.app.rating

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.frameout.app.BuildConfig
import com.frameout.core.util.RatingStrings

object RatingConfig {
    const val FEEDBACK_EMAIL: String = com.frameout.app.about.AboutConfig.CONTACT_EMAIL
}

object StoreLauncher {
    fun openStore(context: Context) {
        val pkg = context.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (e: ActivityNotFoundException) {
            try { context.startActivity(web) } catch (_: ActivityNotFoundException) { }
        }
    }

    fun sendFeedback(context: Context, stars: Int) {
        val body = buildString {
            append("\n\n---\n")
            append("Frame out ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
            append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Rating: $stars/5\n")
        }
        val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            if (RatingConfig.FEEDBACK_EMAIL.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(RatingConfig.FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, RatingStrings.feedbackSubject())
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(mail) } catch (_: ActivityNotFoundException) { }
    }
}
