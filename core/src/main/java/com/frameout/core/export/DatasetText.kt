package com.frameout.core.export

import com.frameout.core.storage.ExifText
import java.util.Locale

object DatasetText {
    const val CSV_NAME = "frames.csv"
    const val README_NAME = "README.txt"
    const val HEADER = "sequence,file,split,time_ms,timecode,sharpness,score,width,height"

    fun csv(entries: List<DatasetEntry>): String {
        val out = StringBuilder(CsvText.BOM).append(HEADER).append(CsvText.EOL)
        entries.forEach { e ->
            val s = e.frame.staged
            out.append(e.sequence).append(',')
                .append(CsvText.quote(e.path)).append(',')
                .append(e.split).append(',')
                .append(s.ptsUs / 1000L).append(',')
                .append(ExifText.position(s.ptsUs)).append(',')
                .append(e.frame.sharpness).append(',')
                .append(score(e.frame.score)).append(',')
                .append(s.width).append(',')
                .append(s.height).append(CsvText.EOL)
        }
        return out.toString()
    }

    fun score(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

    fun jsonString(value: String): String {
        val out = StringBuilder("\"")
        value.forEach { c ->
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' -> out.append(String.format(Locale.ROOT, "\\u%04x", c.code))
                else -> out.append(c)
            }
        }
        return out.append('"').toString()
    }

    fun yoloYaml(): String = "path: .\ntrain: images/train\nval: images/val\nnc: 1\nnames:\n  0: object\n"

    fun classesTxt(): String = "object\n"
}
