package com.frameout.core.export

import com.frameout.core.storage.ExifText

class FrameRow(
    val sequence: Int,
    val frameIndex: Int,
    val frameId: Long,
    val ptsUs: Long,
    val keyframe: Boolean,
    val favorite: Boolean,
    val source: String
)

object FramesTable {
    const val MIME = "text/csv"
    const val HEADER = "sequence,frame_index,frame_id,time_ms,timecode,keyframe,favorite,video"

    fun csv(rows: List<FrameRow>): String {
        val out = StringBuilder(CsvText.BOM).append(HEADER).append(CsvText.EOL)
        rows.forEach { r ->
            out.append(r.sequence).append(',')
                .append(r.frameIndex).append(',')
                .append(r.frameId).append(',')
                .append(r.ptsUs / 1000L).append(',')
                .append(ExifText.position(r.ptsUs)).append(',')
                .append(if (r.keyframe) 1 else 0).append(',')
                .append(if (r.favorite) 1 else 0).append(',')
                .append(CsvText.quote(r.source)).append(CsvText.EOL)
        }
        return out.toString()
    }
}
