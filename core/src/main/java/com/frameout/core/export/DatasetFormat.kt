package com.frameout.core.export

import java.util.Locale

enum class DatasetFormat(val key: String) {
    Colmap("colmap"),
    Yolo("yolo"),
    Coco("coco")
}

class DatasetFrame(val staged: StagedFrame, val sharpness: Int, val score: Double, val rejected: Boolean = false) {
    val extension: String get() = staged.file.extension.lowercase(Locale.ROOT).ifBlank { "jpg" }
}

object DatasetSplit {
    const val VAL_EVERY = 5

    fun isVal(position: Int, total: Int): Boolean {
        if (total < 2) return false
        if (total < VAL_EVERY) return position == total - 1
        return (position + 1) % VAL_EVERY == 0
    }
}

class DatasetEntry(val sequence: Int, val frame: DatasetFrame, val path: String, val split: String, val base: String)

object DatasetNaming {
    fun base(sequence: Int): String = String.format(Locale.ROOT, "frame_%06d", sequence)

    const val REJECTED = "rejected"

    fun entries(format: DatasetFormat, frames: List<DatasetFrame>): List<DatasetEntry> {
        val kept = frames.filter { !it.rejected }
        val own = kept.mapIndexed { position, frame ->
            val base = base(position + 1)
            val split = if (format == DatasetFormat.Colmap) "" else if (DatasetSplit.isVal(position, kept.size)) "val" else "train"
            val folder = if (split.isEmpty()) "images" else "images/$split"
            DatasetEntry(position + 1, frame, "$folder/$base.${frame.extension}", split, base)
        }
        val dropped = frames.filter { it.rejected }.mapIndexed { i, frame ->
            val sequence = kept.size + i + 1
            val base = base(sequence)
            DatasetEntry(sequence, frame, "$REJECTED/$base.${frame.extension}", REJECTED, base)
        }
        return own + dropped
    }
}
