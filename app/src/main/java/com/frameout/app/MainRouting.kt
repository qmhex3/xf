package com.frameout.app

import android.net.Uri
import androidx.activity.ComponentActivity
import java.util.concurrent.CancellationException

internal fun contentKeyOf(route: AppRoute): Any = when (route) {
    is AppRoute.Extracting -> "project-${route.projectId}"
    is AppRoute.Frames -> "project-${route.projectId}"
    else -> route
}

internal val ACTIVE_EXTRACTION = setOf("Queued", "Indexing", "Extracting", "Paused")

internal suspend fun startExtraction(activity: ComponentActivity, app: FrameOutApp, uri: Uri, range: com.frameout.engine.timeline.TrimRange, autoSave: Boolean, onError: (String) -> Unit): Long? = try {
    launchExtract(activity, app.container, uri, range, autoSave)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    com.frameout.core.util.DiagLog.append("extract", "start failed: ${e.javaClass.simpleName}: ${e.message}")
    onError(com.frameout.core.util.FrameStrings.extractStartFailed())
    null
}

internal fun pickerRoute(uris: List<Uri>): AppRoute = when {
    uris.isEmpty() -> AppRoute.Library
    uris.size == 1 -> AppRoute.Trim(uris.first())
    else -> AppRoute.Batch(uris.map(Uri::toString))
}
