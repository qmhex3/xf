package com.frameout.engine.work

import com.frameout.core.db.AppDatabase
import com.frameout.core.db.ProjectEntity
import com.frameout.core.prefs.AppPrefs
import com.frameout.engine.extract.ExtractModeCodec
import com.frameout.engine.extract.ExtractRequest
import com.frameout.engine.extract.GateFlags
import com.frameout.engine.extract.SmartKeyframes

internal suspend fun ExtractWorker.buildRequest(projectId: Long, project: ProjectEntity, db: AppDatabase): ExtractRequest {
    if (project.extractedCount == 0) {
        com.frameout.core.quality.SkipStats.reset(projectId)
        SmartKeyframes.reset(applicationContext, projectId)
    }
    var flags = project.gateFlags
    if (!GateFlags.isStored(flags)) {
        val prefs = AppPrefs(applicationContext)
        val smart = com.frameout.core.pro.ProAccess.permits(com.frameout.core.pro.ProFeature.SmartExtract)
        flags = GateFlags.encode(smart && prefs.skipBlurry.value, smart && prefs.skipDuplicates.value, smart && prefs.peopleOnly.value, prefs.peopleBody.value)
        db.projectDao().setGateFlags(projectId, flags)
    }
    return ExtractRequest(
        projectId,
        project.sourceUri,
        project.startMs,
        project.endMs,
        ExtractModeCodec.decode(project.extractMode),
        project.autoSave,
        inputData.getString(ExtractWorker.KEY_SAVE_FORMAT) ?: project.saveFormat,
        inputData.getInt(ExtractWorker.KEY_JPEG_QUALITY, project.jpegQuality).coerceIn(1, 100),
        skipBlurry = GateFlags.blurry(flags),
        skipDuplicates = GateFlags.duplicates(flags),
        peopleOnly = GateFlags.people(flags),
        peopleBody = GateFlags.body(flags)
    )
}
