package com.frameout.app

import android.content.Context
import android.net.Uri
import com.frameout.app.trim.estimateFrames
import com.frameout.core.db.ProjectEntity
import com.frameout.core.util.AppContainer
import com.frameout.core.util.FrameStrings
import com.frameout.engine.extract.ExtractModeCodec
import com.frameout.engine.extract.ExtractRequest
import com.frameout.engine.timeline.TrimRange
import com.frameout.engine.work.ExtractScheduler

suspend fun launchExtract(
    context: Context,
    container: AppContainer,
    uri: Uri,
    range: TrimRange,
    autoSave: Boolean
): Long {
    val prefs = container.prefs
    val useAuto = autoSave || prefs.autoSave.value
    val modePref = prefs.extractMode.value
    val mode = ExtractModeCodec.decode(modePref).takeIf { allowedMode(it) } ?: com.frameout.engine.extract.ExtractMode.EveryFrame
    val startMs = if (range.useFull) 0 else range.startMs
    val endMs = range.endMs
    val estFrames = estimateFrames(if (range.useFull) range.endMs else range.spanMs(), mode)
    if (!container.storage.hasSpaceForResolution(estFrames, 0, 0, useAuto, prefs.saveFormat.value)) error(FrameStrings.lowStorage())
    val name = uri.lastPathSegment ?: "video"
    val now = System.currentTimeMillis()
    val projectId = container.projects.insert(
        ProjectEntity(
            name = name,
            sourceUri = uri.toString(),
            sourceName = name,
            createdAt = now,
            updatedAt = now,
            extractMode = ExtractModeCodec.encode(mode),
            autoSave = useAuto,
            saveFormat = prefs.saveFormat.value,
            jpegQuality = prefs.jpegQuality.value,
            startMs = startMs,
            endMs = range.endMs
        )
    )
    ProjectCover.create(context, container.storage, container.projects, projectId, uri, startMs)
    ExtractScheduler.enqueue(context, ExtractRequest(projectId, uri.toString(), startMs, endMs, mode, useAuto, prefs.saveFormat.value, prefs.jpegQuality.value))
    return projectId
}


private fun allowedMode(mode: com.frameout.engine.extract.ExtractMode): Boolean {
    val smart = mode == com.frameout.engine.extract.ExtractMode.SceneChange || mode == com.frameout.engine.extract.ExtractMode.Slides
    return !smart || com.frameout.core.pro.ProAccess.permits(com.frameout.core.pro.ProFeature.SmartExtract)
}
