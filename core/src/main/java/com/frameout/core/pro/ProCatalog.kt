package com.frameout.core.pro

import com.frameout.core.storage.ImageFormat

object ProCatalog {
    val freeFilters: Set<String> = setOf("", "mono", "sepia", "warm")
    val freeCollages: Set<String> = setOf("2x1", "1x2", "2x2")
    val freeAccents: Set<String> = setOf("indigo", "ocean")
    val proOnlyAccents: Set<String> = setOf("black", "dynamic")
    val freeFormats: Set<ImageFormat> = setOf(ImageFormat.Jpg, ImageFormat.Png, ImageFormat.WebpLossy)
    const val TRIALS_PER_FEATURE = 3
    const val GRACE_DAYS = 14

    fun filterIsFree(id: String): Boolean = id in freeFilters
    fun collageIsFree(id: String): Boolean = id in freeCollages
    fun accentIsFree(key: String): Boolean = key in freeAccents
    fun accentIsProOnly(key: String): Boolean = key in proOnlyAccents
    fun formatIsFree(format: ImageFormat): Boolean = format in freeFormats
}
