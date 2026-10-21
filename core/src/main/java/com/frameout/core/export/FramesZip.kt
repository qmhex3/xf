package com.frameout.core.export

import com.frameout.core.storage.ExifText
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream

class StagedFrame(
    val name: String,
    val file: File,
    val frameId: Long,
    val frameIndex: Int,
    val sequence: Int,
    val ptsUs: Long,
    val width: Int,
    val height: Int
) {
    fun withName(newName: String): StagedFrame = StagedFrame(newName, file, frameId, frameIndex, sequence, ptsUs, width, height)
}

object FramesZip {
    const val CSV_NAME = "metadata.csv"
    const val JSON_NAME = "metadata.json"
    private const val HEADER = "sequence,file,frame_index,frame_id,time_ms,timecode,width,height,bytes"

    fun write(frames: List<StagedFrame>, project: String, out: OutputStream) {
        val entries = ArchiveNames.resolve(frames, setOf(CSV_NAME, JSON_NAME))
        ZipSession.use(out) { zip ->
            ZipSession.putBytes(zip, CSV_NAME, csv(entries).toByteArray(Charsets.UTF_8))
            ZipSession.putBytes(zip, JSON_NAME, json(entries, project).toByteArray(Charsets.UTF_8))
            entries.forEach { ZipSession.putFrame(zip, it) }
        }
    }

    fun csv(frames: List<StagedFrame>): String {
        val builder = StringBuilder("\uFEFF").append(HEADER).append("\r\n")
        frames.forEach { f ->
            builder.append(f.sequence).append(',')
                .append(CsvText.quote(f.name)).append(',')
                .append(f.frameIndex).append(',')
                .append(f.frameId).append(',')
                .append(f.ptsUs / 1000L).append(',')
                .append(ExifText.position(f.ptsUs)).append(',')
                .append(f.width).append(',')
                .append(f.height).append(',')
                .append(f.file.length()).append("\r\n")
        }
        return builder.toString()
    }

    fun json(frames: List<StagedFrame>, project: String): String {
        val items = JSONArray()
        frames.forEach { f ->
            items.put(
                JSONObject()
                    .put("sequence", f.sequence)
                    .put("file", f.name)
                    .put("frame_index", f.frameIndex)
                    .put("frame_id", f.frameId)
                    .put("time_ms", f.ptsUs / 1000L)
                    .put("timecode", ExifText.position(f.ptsUs))
                    .put("width", f.width)
                    .put("height", f.height)
                    .put("bytes", f.file.length())
            )
        }
        return JSONObject().put("project", project).put("count", frames.size).put("frames", items).toString(2)
    }
}
