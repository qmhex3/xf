package com.frameout.app.full

import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FormatProbe
import java.io.File

class Fixture(val file: File, val frames: Int) {
    val name: String get() = file.name
    val uri: Uri get() = Uri.fromFile(file)
}

object Fixtures {
    fun dir(context: Context): File = File(context.filesDir, "fixtures")

    fun all(context: Context): List<Fixture> {
        val manifest = File(dir(context), "manifest.txt")
        if (!manifest.isFile) return emptyList()
        return manifest.readLines().mapNotNull { line ->
            val parts = line.trim().split(' ')
            val file = File(dir(context), parts.firstOrNull().orEmpty())
            val frames = parts.getOrNull(1)?.toIntOrNull()
            if (parts.size == 2 && frames != null && file.isFile) Fixture(file, frames) else null
        }
    }

    fun names(): List<String> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return all(context).map { it.name }
    }

    fun find(context: Context, name: String): Fixture? = all(context).firstOrNull { it.name == name }

    fun needingFfmpeg(context: Context): List<Fixture> =
        all(context).filter { FormatProbe.probe(context, it.uri).kind == FormatKind.NeedsFfmpeg }
}
