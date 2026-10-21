package com.frameout.app.search

import com.frameout.app.asChooserWithoutSelf
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.frameout.core.db.FrameEntity
import com.frameout.core.search.ImageSearchEngines
import com.frameout.core.search.SearchEngine
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.SearchStrings

internal const val SEARCH_HANDOFF_MS = 1500L

object ImageSearchLauncher {
    private const val LENS_PACKAGE = "com.google.ar.lens"

    suspend fun run(context: Context, exporter: FrameExporter, engine: SearchEngine, frame: FrameEntity): String? {
        if (engine.id == ImageSearchEngines.CHOOSER_ID) return if (sendToChooser(context, exporter, frame)) null else SearchStrings.failed()
        if (engine.supportsShare && sendToApp(context, exporter, engine, frame)) return null
        return openSite(context, exporter, engine, frame)
    }

    private suspend fun sendToChooser(context: Context, exporter: FrameExporter, frame: FrameEntity): Boolean {
        val share = exporter.shareIntent(listOf(frame), includeLocation = false) ?: return false
        val uri = share.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return false
        share.clipData = ClipData.newRawUri("", uri)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return start(context, share.asChooserWithoutSelf(context, SearchStrings.title()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private suspend fun sendToApp(context: Context, exporter: FrameExporter, engine: SearchEngine, frame: FrameEntity): Boolean {
        val share = exporter.shareIntent(listOf(frame), includeLocation = false) ?: return false
        val uri = share.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return false
        share.clipData = ClipData.newRawUri("", uri)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val targeted = target(context, share, engine) ?: return false
        return start(context, targeted)
    }

    private suspend fun openSite(context: Context, exporter: FrameExporter, engine: SearchEngine, frame: FrameEntity): String? {
        val share = exporter.shareIntent(listOf(frame), includeLocation = false) ?: return SearchStrings.failed()
        val uri = share.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return SearchStrings.failed()
        val intent = Intent(context, ImageSearchActivity::class.java)
            .putExtra(ImageSearchActivity.EXTRA_URI, uri.toString())
            .putExtra(ImageSearchActivity.EXTRA_ENGINE, engine.id)
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (start(context, intent)) null else SearchStrings.failed()
    }

    @Suppress("DEPRECATION")
    private fun target(context: Context, share: Intent, engine: SearchEngine): Intent? {
        val candidates = context.packageManager.queryIntentActivities(Intent(share).apply { clipData = null }, 0)
        val inPackages = candidates.filter { it.activityInfo.packageName in engine.packages && it.activityInfo.exported }
        val match = inPackages.firstOrNull { it.activityInfo.packageName == LENS_PACKAGE }
            ?: inPackages.firstOrNull { it.activityInfo.name.contains("lens", ignoreCase = true) }
            ?: inPackages.firstOrNull()
            ?: return null
        return Intent(share).setClassName(match.activityInfo.packageName, match.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}
