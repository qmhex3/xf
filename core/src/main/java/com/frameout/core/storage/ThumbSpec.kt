package com.frameout.core.storage

object ThumbSpec {
    const val MAX_SIDE = 512
    const val JPEG_QUALITY = 85
    const val ANALYSIS_SIDE = 1024
    const val ANALYSIS_SIDE_REDUCED = 768
    const val ANALYSIS_QUALITY = 80
    const val ANALYSIS_FOLDER = "analysis"

    fun analysisName(index: Int): String = "a_$index.jpg"
}
