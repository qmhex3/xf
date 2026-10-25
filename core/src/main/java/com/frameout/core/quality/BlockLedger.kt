package com.frameout.core.quality

import java.io.File

object BlockLedger {
    const val FOLDER = "quality"
    const val NAME = "blocks.tsv"
    private const val SCALE = 1000
    private const val THUMBS_FOLDER = "thumbs"
    private val lock = Any()

    fun fileIn(projectDir: File): File = File(File(projectDir, FOLDER), NAME)

    fun projectDirOf(thumbnailPath: String?): File? {
        if (thumbnailPath.isNullOrBlank()) return null
        val folder = File(thumbnailPath).parentFile ?: return null
        return if (folder.name == THUMBS_FOLDER) folder.parentFile else null
    }

    fun record(projectDir: File, index: Int, factor: Double) {
        val permille = (factor * SCALE).toInt().coerceIn(0, SCALE)
        if (permille >= SCALE) return
        val file = fileIn(projectDir)
        synchronized(lock) {
            file.parentFile?.mkdirs()
            file.appendText("$index\t$permille\n")
        }
    }

    fun read(projectDir: File): Map<Int, Double> {
        val file = fileIn(projectDir)
        val text = synchronized(lock) { if (file.isFile) file.readText() else return emptyMap() }
        val out = HashMap<Int, Double>()
        for (line in text.lineSequence()) parse(line)?.let { out[it.first] = it.second }
        return out
    }

    private fun parse(line: String): Pair<Int, Double>? {
        val parts = line.split('\t')
        if (parts.size != 2) return null
        val index = parts[0].toIntOrNull() ?: return null
        val permille = parts[1].toIntOrNull() ?: return null
        return index to permille.coerceIn(0, SCALE) / SCALE.toDouble()
    }

    class Lookup {
        private var dir: File? = null
        private var factors: Map<Int, Double> = emptyMap()

        fun factor(thumbnailPath: String?, index: Int): Double {
            val folder = projectDirOf(thumbnailPath) ?: return 1.0
            if (folder != dir) {
                dir = folder
                factors = runCatching { read(folder) }.getOrDefault(emptyMap())
            }
            return factors[index] ?: 1.0
        }
    }
}
