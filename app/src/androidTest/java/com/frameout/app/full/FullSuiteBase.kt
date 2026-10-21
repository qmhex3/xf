package com.frameout.app.full

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.app.GeneratedVideo
import com.frameout.app.VideoSpec
import com.frameout.core.db.AppDatabase
import com.frameout.core.db.ProjectEntity
import com.frameout.core.storage.StorageManager
import com.frameout.engine.extract.CheckpointStore
import com.frameout.engine.extract.ExtractEngine
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractRequest
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After

abstract class FullSuiteBase {
    protected val context = InstrumentationRegistry.getInstrumentation().targetContext
    protected val db = AppDatabase.get(context)
    protected val storage = StorageManager(context)
    private val files = mutableListOf<File>()
    private val projects = mutableListOf<Long>()

    @After
    fun cleanupFullSuite() = runBlocking {
        projects.forEach {
            db.videoIndexDao().clearIndex(it)
            db.projectDao().delete(it)
            storage.deleteProject(it)
        }
        files.forEach { it.delete() }
    }

    protected fun track(file: File): File = file.also { files += it }

    protected fun generate(spec: VideoSpec): File {
        val file = track(File(context.cacheDir, "full_${System.nanoTime()}.${spec.extension}"))
        GeneratedVideo.write(file, spec)
        return file
    }

    protected suspend fun project(file: File): Long {
        val now = System.currentTimeMillis()
        val entity = ProjectEntity(name = "full", sourceUri = Uri.fromFile(file).toString(), sourceName = file.name, createdAt = now, updatedAt = now)
        return db.projectDao().insert(entity).also { projects += it }
    }

    protected fun request(file: File, id: Long) = ExtractRequest(id, Uri.fromFile(file).toString(), 0L, 0L, ExtractMode.EveryFrame, false)

    protected fun engine() = ExtractEngine(context, db.projectDao(), db.frameDao(), storage, db.videoIndexDao(), CheckpointStore(db))

    protected suspend fun extract(file: File): Long {
        val id = project(file)
        engine().run(request(file, id))
        return id
    }

    protected suspend fun status(id: Long): String? = db.projectDao().getById(id)?.status

    protected fun relative(values: List<Long>): List<Long> = values.sorted().let { sorted -> sorted.map { it - sorted.first() } }

    protected fun constant(frames: Int, width: Int = 320, height: Int = 240, mime: String = "video/avc", rotation: Int = 0) =
        VideoSpec(frames, { it * 33_333L }, 0, width, height, mime, rotation)
}
