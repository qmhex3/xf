package com.frameout.core.db

import com.frameout.core.media.MotionPhotoExtractor

enum class SavedKind(val code: Int) {
    Photo(0),
    Gif(1),
    Motion(2)
}

data class SavedKindCount(val kind: Int, val total: Int)

const val SAVED_KIND_SQL: String =
    "CASE WHEN LOWER(displayName) LIKE '%.gif' THEN 1 " +
        "WHEN (LOWER(displayName) LIKE 'mvimg%' OR INSTR(LOWER(displayName), '.mp.') > 0 OR LOWER(displayName) LIKE '%!_mp.jpg' ESCAPE '!') THEN 2 " +
        "ELSE 0 END"

fun savedKindOf(displayName: String): SavedKind {
    val name = displayName.lowercase()
    return when {
        name.endsWith(".gif") -> SavedKind.Gif
        MotionPhotoExtractor.isLikelyMotionName(name) -> SavedKind.Motion
        else -> SavedKind.Photo
    }
}
