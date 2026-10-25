package com.frameout.app

import android.net.Uri
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import java.net.URLDecoder
import java.net.URLEncoder

object RouteCodec {
    private const val SEP = "|"
    private const val MAX_IDS = 500

    fun encode(route: AppRoute): String? = when (route) {
        AppRoute.Library -> "library"
        AppRoute.Settings -> "settings"
        AppRoute.About -> "about"
        AppRoute.Picker -> "picker"
        is AppRoute.Trim -> join("trim", escape(route.uri.toString()))
        is AppRoute.Extracting -> join("extracting", route.projectId)
        is AppRoute.Frames -> if (route.focusFrameId != null) join("frames", route.projectId, route.focusFrameId) else join("frames", route.projectId)
        is AppRoute.Viewer -> if (route.savedId != null) join("viewer", route.projectId, route.frameId, route.savedId) else join("viewer", route.projectId, route.frameId)
        is AppRoute.Collage -> ids(route.frameIds)?.let { join("collage", route.projectId, it) } ?: join("frames", route.projectId)
        is AppRoute.Compare -> ids(route.frameIds)?.let { join("compare", route.projectId, it) } ?: join("frames", route.projectId)
        is AppRoute.SavedViewer -> join("savedviewer", route.savedId)
        is AppRoute.Batch -> null
    }

    fun decode(text: String): AppRoute? = runCatching { parse(text.split(SEP)) }.getOrNull()

    fun restore(saved: List<String>): List<AppRoute> {
        val routes = saved.mapNotNull(::decode)
        return if (routes.firstOrNull() == AppRoute.Library) routes else listOf<AppRoute>(AppRoute.Library) + routes
    }

    private fun parse(p: List<String>): AppRoute? = when (p[0]) {
        "library" -> AppRoute.Library
        "settings" -> AppRoute.Settings
        "about" -> AppRoute.About
        "picker" -> AppRoute.Picker
        "trim" -> AppRoute.Trim(Uri.parse(unescape(p[1])))
        "extracting" -> AppRoute.Extracting(p[1].toLong())
        "frames" -> AppRoute.Frames(p[1].toLong(), p.getOrNull(2)?.toLongOrNull())
        "viewer" -> AppRoute.Viewer(p[1].toLong(), p[2].toLong(), p.getOrNull(3)?.toLongOrNull())
        "savedviewer" -> AppRoute.SavedViewer(p[1].toLong())
        "collage" -> AppRoute.Collage(p[1].toLong(), parseIds(p[2]))
        "compare" -> AppRoute.Compare(p[1].toLong(), parseIds(p[2]))
        else -> null
    }

    private fun join(vararg parts: Any): String = parts.joinToString(SEP)

    private fun ids(list: List<Long>): String? =
        if (list.isEmpty() || list.size > MAX_IDS) null else list.joinToString(",")

    private fun parseIds(text: String): List<Long> = text.split(",").map(String::toLong)

    private fun escape(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun unescape(value: String): String = URLDecoder.decode(value, "UTF-8")
}

val routeStackSaver: Saver<SnapshotStateList<AppRoute>, Any> = listSaver<SnapshotStateList<AppRoute>, String>(
    save = { stack -> stack.mapNotNull(RouteCodec::encode) },
    restore = { saved -> RouteCodec.restore(saved).toMutableStateList() }
)
