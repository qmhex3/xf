package com.frameout.core.backup

import com.frameout.core.backup.BackupText.LF
import com.frameout.core.backup.BackupText.TAB

object BackupManifest {
    private const val FRAMES = "frames"
    private val COLUMNS = listOf("index", "pts_us", "key", "fav", "del", "has_edit", "rot", "flip_h", "flip_v", "crop_l", "crop_t", "crop_r", "crop_b", "bright", "contrast", "sat", "edit_del", "marks", "filter", "filter_str")

    fun encode(data: BackupData): String {
        val out = StringBuilder(64 + data.frames.size * 40)
        out.append(BackupFormat.MAGIC).append(TAB).append(BackupFormat.VERSION).append(LF)
        BackupFields.write(data.project).forEach { (key, value) -> out.append(key).append(TAB).append(BackupText.escape(value)).append(LF) }
        out.append(FRAMES).append(LF).append(COLUMNS.joinToString(TAB.toString())).append(LF)
        data.frames.forEach { out.append(row(it)).append(LF) }
        return out.toString()
    }

    fun decode(text: String): BackupData = decode(text.lineSequence().iterator())

    fun decode(lines: Iterator<String>): BackupData {
        try {
            checkHeader(if (lines.hasNext()) lines.next() else "")
            val fields = HashMap<String, String>()
            while (lines.hasNext()) {
                val line = lines.next()
                if (line == FRAMES) break
                val cut = line.indexOf(TAB)
                if (cut > 0) fields[line.substring(0, cut)] = BackupText.unescape(line.substring(cut + 1))
            }
            return BackupData(BackupFields.read(fields), readFrames(lines))
        } catch (e: NumberFormatException) {
            throw BackupException(BackupException.Reason.Invalid)
        }
    }

    private fun checkHeader(line: String) {
        val parts = line.split(TAB)
        if (parts.size != 2 || parts[0] != BackupFormat.MAGIC) throw BackupException(BackupException.Reason.Invalid)
        val version = parts[1].toIntOrNull() ?: throw BackupException(BackupException.Reason.Invalid)
        if (version > BackupFormat.VERSION) throw BackupException(BackupException.Reason.Newer)
    }

    private fun readFrames(lines: Iterator<String>): List<BackupFrame> {
        if (!lines.hasNext()) return emptyList()
        val header = lines.next().split(TAB)
        val columns = header.withIndex().associate { it.value to it.index }
        if ("index" !in columns || "pts_us" !in columns) throw BackupException(BackupException.Reason.Invalid)
        val frames = ArrayList<BackupFrame>()
        while (lines.hasNext()) {
            val line = lines.next()
            if (line.isEmpty()) continue
            if (frames.size >= BackupFormat.MAX_FRAMES) throw BackupException(BackupException.Reason.TooLarge)
            frames.add(BackupRows.parse(columns, line.split(TAB)))
        }
        return frames
    }

    private fun row(f: BackupFrame): String = BackupRows.format(f)
}
