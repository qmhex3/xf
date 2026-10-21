package com.frameout.core.util

import android.content.Context
import com.frameout.codecpack.MediaFailure
import java.io.File

object FailureMarker {
    private const val DIR = "failures"

    private fun file(context: Context, projectId: Long): File = File(File(context.filesDir, DIR), "$projectId.txt")

    fun write(context: Context, projectId: Long, reason: MediaFailure) {
        runCatching {
            val target = file(context, projectId)
            target.parentFile?.mkdirs()
            target.writeText(reason.name)
        }
    }

    fun read(context: Context, projectId: Long): MediaFailure? = runCatching {
        MediaFailure.fromName(file(context, projectId).readText().trim())
    }.getOrNull()

    fun clear(context: Context, projectId: Long) {
        runCatching { file(context, projectId).delete() }
    }
}
