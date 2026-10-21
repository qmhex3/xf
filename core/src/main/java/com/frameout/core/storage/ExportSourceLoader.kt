package com.frameout.core.storage

import com.frameout.core.util.logged
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectDao
import com.frameout.core.db.VideoIndexDao
import com.frameout.core.edit.GainmapOps
import com.frameout.core.hdr.HdrSupport
import com.frameout.core.media.MotionPhotoExtractor
import java.io.File

class ExportSourceLoader(private val context: Context, private val projectDao: ProjectDao, videoIndexDao: VideoIndexDao?) {
    private val decoder = FrameSourceDecoder(context, videoIndexDao)
    private val sources = HashMap<Long, Uri?>()

    @Synchronized
    fun release() {
        sources.clear()
        decoder.close()
    }

    suspend fun load(frame: FrameEntity): Bitmap? {
        val hdr = HdrSupport.enabled(context)
        if (HdrSupport.exact(context)) {
            val exact = exactFrame(frame)
            if (exact != null) return exact
        }
        val disk = decodeDisk(frame, hdr)
        if (disk != null && (!hdr || GainmapOps.has(disk))) return disk
        val source = videoSource(frame)
        val fetched = if (hdr && source != null) HdrSupport.source?.fetch(source, frame.ptsUs) else null
        if (fetched != null) {
            disk?.recycle()
            return fetched
        }
        return disk ?: source?.let { decoder.decode(frame, it) }
    }

    private suspend fun exactFrame(frame: FrameEntity): Bitmap? {
        val source = videoSource(frame) ?: return null
        val hdr = HdrSupport.source ?: return null
        return hdr.fetchExact(source, frame.ptsUs) ?: if (hdr.isHdrSource(source)) decoder.decodeExact(frame, source) else null
    }

    private fun decodeDisk(frame: FrameEntity, hdr: Boolean): Bitmap? {
        val path = frame.fullPath?.takeIf { File(it).exists() } ?: return null
        return runCatching { if (hdr) GainmapOps.decodeFile(path) else BitmapFactory.decodeFile(path) }.logged("ExportSourceLoader").getOrNull()
    }

    private suspend fun videoSource(frame: FrameEntity): Uri? {
        synchronized(this) { if (sources.containsKey(frame.projectId)) return sources[frame.projectId] }
        val project = projectDao.getById(frame.projectId)
        val resolved = project?.let { MotionPhotoExtractor.resolveVideoSource(context, Uri.parse(it.sourceUri)) }
        synchronized(this) { sources[frame.projectId] = resolved }
        return resolved
    }
}
