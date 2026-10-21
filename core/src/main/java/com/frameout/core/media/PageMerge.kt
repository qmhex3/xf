package com.frameout.core.media

data class MediaKey(val dateAdded: Long, val id: Long) : Comparable<MediaKey> {
    override fun compareTo(other: MediaKey): Int = compareValuesBy(this, other, MediaKey::dateAdded, MediaKey::id)
}

class MergePlan<T>(val folder: List<T>, val bound: MediaKey?) {
    val next: MediaKey? get() = bound

    fun accepts(key: MediaKey): Boolean = bound == null || key >= bound
}

object PageMerge {
    fun <T> plan(storeLast: MediaKey?, storeFull: Boolean, folder: List<T>, cursor: MediaKey?, limit: Int, keyOf: (T) -> MediaKey): MergePlan<T> {
        val remaining = if (cursor == null) folder else folder.filter { keyOf(it) < cursor }
        val folderBound = if (remaining.size > limit) keyOf(remaining[limit - 1]) else null
        val storeBound = if (storeFull) storeLast else null
        val bound = listOfNotNull(storeBound, folderBound).maxOrNull()
        val included = if (bound == null) remaining else remaining.filter { keyOf(it) >= bound }
        return MergePlan(included, bound)
    }
}
