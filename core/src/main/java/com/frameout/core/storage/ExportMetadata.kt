package com.frameout.core.storage

import android.content.Context
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectDao
import com.frameout.core.prefs.AppPrefs

internal class ExportMetadata(
    private val context: Context,
    private val projectDao: ProjectDao,
    private val prefs: AppPrefs
) {
    private val locations = HashMap<String, GeoPoint?>()

    suspend fun subfolderOf(frame: FrameEntity): String = projectDao.getById(frame.projectId)?.saveSubfolder.orEmpty()

    suspend fun stampFor(createdAt: Long, frame: FrameEntity, includeLocation: Boolean = true): ExifStamp {
        val project = projectDao.getById(frame.projectId)
        val keep = includeLocation && prefs.keepLocation.value
        val location = if (keep) locationOf(project?.sourceUri) else null
        return ExifStamp(createdAt, ExifText.description(project?.name.orEmpty(), frame.index, frame.ptsUs), location, keep)
    }

    private fun locationOf(sourceUri: String?): GeoPoint? {
        if (sourceUri.isNullOrBlank()) return null
        return synchronized(locations) {
            if (locations.containsKey(sourceUri)) locations[sourceUri] else SourceLocation.read(context, sourceUri).also { locations[sourceUri] = it }
        }
    }

    suspend fun exifBlock(format: ImageFormat, createdAt: Long, frame: FrameEntity, includeLocation: Boolean = true): ByteArray? =
        FormatMetadata.block(format, stampFor(createdAt, frame, includeLocation))

    suspend fun createdAt(frame: FrameEntity): Long {
        val project = projectDao.getById(frame.projectId)
        val base = SourceDate.read(context, project?.sourceUri) ?: project?.createdAt ?: System.currentTimeMillis()
        return if (prefs.datePolicy.value == "video") base else base + frame.ptsUs / 1000L
    }
}
