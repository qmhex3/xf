package com.frameout.core.backup

internal object BackupFields {
    fun write(p: BackupProject): List<Pair<String, String>> = listOf(
        "name" to p.name,
        "source_name" to p.sourceName,
        "source_uri" to p.sourceUri,
        "duration_ms" to p.durationMs.toString(),
        "extract_mode" to p.extractMode,
        "save_format" to p.saveFormat,
        "jpeg_quality" to p.jpegQuality.toString(),
        "start_ms" to p.startMs.toString(),
        "end_ms" to p.endMs.toString(),
        "save_subfolder" to p.saveSubfolder,
        "created_at" to p.createdAt.toString()
    )

    fun read(f: Map<String, String>): BackupProject {
        val name = f["name"] ?: throw BackupException(BackupException.Reason.Invalid)
        return BackupProject(
            name = name,
            sourceName = f["source_name"].orEmpty(),
            sourceUri = f["source_uri"].orEmpty(),
            durationMs = f["duration_ms"]?.toLong() ?: 0L,
            extractMode = f["extract_mode"] ?: "EveryFrame",
            saveFormat = f["save_format"] ?: "jpg",
            jpegQuality = f["jpeg_quality"]?.toInt() ?: 92,
            startMs = f["start_ms"]?.toLong() ?: 0L,
            endMs = f["end_ms"]?.toLong() ?: 0L,
            saveSubfolder = f["save_subfolder"].orEmpty(),
            createdAt = f["created_at"]?.toLong() ?: System.currentTimeMillis()
        )
    }
}
