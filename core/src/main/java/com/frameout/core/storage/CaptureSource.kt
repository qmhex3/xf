package com.frameout.core.storage

data class CaptureSource(val uri: String, val positionMs: Long, val sequence: Int = 1)

object CaptureNaming {
    const val DEFAULT = "FrameOut_{project}_{time}"

    fun name(template: String, project: String, positionMs: Long, sequence: Int, ext: String): String {
        val clean = FrameNaming.cleanTemplate(template)
        val base = when {
            clean.isBlank() || clean == FrameNaming.DEFAULT -> DEFAULT
            "{time}" in clean || "{n}" in clean -> clean
            else -> "${clean}_{time}"
        }
        return FrameNaming.render(base, project, 0L, sequence, positionMs.coerceAtLeast(0L) * 1000L, sequence, sequence, ext)
    }
}
