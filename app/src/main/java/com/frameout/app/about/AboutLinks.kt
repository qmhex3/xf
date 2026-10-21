package com.frameout.app.about

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.frameout.app.BuildConfig
import com.frameout.core.util.ModeStrings

internal object AboutLinks {
    fun openYoutube(context: Context) {
        if (!AboutConfig.hasYoutube) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AboutConfig.YOUTUBE_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: ActivityNotFoundException) { }
    }

    fun writeEmail(context: Context) {
        if (!AboutConfig.hasEmail) return
        val body = buildString {
            append("\n\n---\n")
            append("Frame out ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
            append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("${Build.MANUFACTURER} ${Build.MODEL}\n")
        }
        val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(AboutConfig.CONTACT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, ModeStrings.aboutEmailSubject())
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(mail)
        } catch (_: ActivityNotFoundException) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("email", AboutConfig.CONTACT_EMAIL))
            Toast.makeText(context, ModeStrings.aboutEmailCopied(), Toast.LENGTH_SHORT).show()
        }
    }
}
