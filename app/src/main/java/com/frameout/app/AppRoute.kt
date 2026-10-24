package com.frameout.app

import android.net.Uri

sealed class AppRoute {
    data object Library : AppRoute()
    data object Settings : AppRoute()
    data object About : AppRoute()
    data object Picker : AppRoute()
    data class Trim(val uri: Uri) : AppRoute()
    data class Extracting(val projectId: Long) : AppRoute()
    data class Frames(val projectId: Long) : AppRoute()
    data class Collage(val projectId: Long, val frameIds: List<Long>) : AppRoute()
    data class Compare(val projectId: Long, val frameIds: List<Long>) : AppRoute()
    data class Viewer(val projectId: Long, val frameId: Long, val savedId: Long? = null) : AppRoute()
    data class SavedViewer(val savedId: Long) : AppRoute()
    data class Batch(val uris: List<String>, val session: Long = System.nanoTime()) : AppRoute()
}
