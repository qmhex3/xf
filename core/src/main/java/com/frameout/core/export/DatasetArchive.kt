package com.frameout.core.export

import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DatasetArchive {
    fun write(format: DatasetFormat, frames: List<DatasetFrame>, project: String, createdAtMs: Long, out: OutputStream) {
        val entries = DatasetNaming.entries(format, frames)
        ZipSession.use(out) { zip ->
            entries.forEach { ZipSession.putFrame(zip, it.frame.staged.withName(it.path)) }
            files(format, entries, project, createdAtMs).forEach { (name, text) ->
                ZipSession.putBytes(zip, name, text.toByteArray(Charsets.UTF_8))
            }
        }
    }

    fun files(format: DatasetFormat, entries: List<DatasetEntry>, project: String, createdAtMs: Long): List<Pair<String, String>> {
        val common = listOf(
            DatasetText.CSV_NAME to DatasetText.csv(entries),
            DatasetText.README_NAME to DatasetReadme.text(format, entries.count { it.split != DatasetNaming.REJECTED }, project, entries.count { it.split == DatasetNaming.REJECTED })
        )
        return common + when (format) {
            DatasetFormat.Colmap -> listOf(DatasetColmap.SH_NAME to DatasetColmap.shell(), DatasetColmap.BAT_NAME to DatasetColmap.batch())
            DatasetFormat.Yolo -> yolo(entries)
            DatasetFormat.Coco -> coco(entries, project, createdAtMs)
        }
    }

    private fun yolo(entries: List<DatasetEntry>): List<Pair<String, String>> =
        listOf("data.yaml" to DatasetText.yoloYaml(), "classes.txt" to DatasetText.classesTxt()) +
            entries.filter { it.split == "train" || it.split == "val" }.map { "labels/${it.split}/${it.base}.txt" to "" }

    private fun coco(entries: List<DatasetEntry>, project: String, createdAtMs: Long): List<Pair<String, String>> {
        val stamp = iso(createdAtMs)
        return listOf("train", "val").filter { split -> entries.any { it.split == split } }.map { split ->
            "annotations/instances_$split.json" to DatasetCoco.annotations(entries, split, project, stamp)
        }
    }

    private fun iso(ms: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(ms))
    }
}
