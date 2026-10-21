package com.frameout.core.quality

import com.frameout.core.storage.ThumbSpec
import java.io.File

object AnalysisSource {
    private const val THUMBS_FOLDER = "thumbs"

    fun needsCopy(width: Int, height: Int): Boolean = maxOf(width, height) > ThumbSpec.MAX_SIDE

    fun copyPath(thumbnailPath: String?, index: Int): String? {
        if (thumbnailPath.isNullOrBlank()) return null
        val folder = File(thumbnailPath).parentFile ?: return null
        if (folder.name != THUMBS_FOLDER) return null
        val project = folder.parentFile ?: return null
        return File(File(project, ThumbSpec.ANALYSIS_FOLDER), ThumbSpec.analysisName(index)).path
    }

    fun candidates(fullPath: String?, thumbnailPath: String?, index: Int): List<String> =
        listOfNotNull(fullPath, copyPath(thumbnailPath, index), thumbnailPath)
            .filter { it.isNotBlank() }
            .distinct()

    fun side(reduced: Boolean): Int = if (reduced) ThumbSpec.ANALYSIS_SIDE_REDUCED else ThumbSpec.ANALYSIS_SIDE
}
