package com.frameout.engine.index

import androidx.room3.withWriteTransaction
import com.frameout.codecpack.FfmpegBridge
import com.frameout.core.db.AppDatabase
import com.frameout.core.db.VideoIndexEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

class FfmpegIndexManager(private val db: AppDatabase) {
    private companion object {
        val nextToken = AtomicLong(0L)
        const val ABORT_POLL_MS = 100L
        const val CHUNK = 2000
    }

    private val dao get() = db.videoIndexDao()

    class Stage(val table: LongArray, val complete: Boolean)

    suspend fun importPrefix(projectId: Long, path: String, maxPackets: Int): Stage? {
        val complete = BooleanArray(1)
        val raw = withContext(Dispatchers.IO) { FfmpegBridge.nativeFrameIndexPrefix(path, maxPackets, complete) }
        if (raw == null || raw.isEmpty() || raw.size % 4 != 0) return null
        replace(projectId, raw)
        return Stage(raw, complete[0])
    }

    suspend fun importFromNative(projectId: Long, path: String, shouldAbort: () -> Boolean = { false }, onScanned: (LongArray) -> Unit = {}): Int {
        val raw = scanNative(path, shouldAbort) ?: return 0
        if (raw.isEmpty() || raw.size % 4 != 0) return 0
        onScanned(raw)
        val frameCount = raw.size / 4
        val existing = dao.count(projectId)
        if (existing == frameCount && existing > 0) return existing
        replace(projectId, raw)
        return frameCount
    }

    private suspend fun replace(projectId: Long, raw: LongArray) {
        val stagingId = -projectId
        dao.clearIndex(stagingId)
        try {
            swapStaging(projectId, stagingId, raw)
        } catch (e: Exception) {
            runCatching { dao.clearIndex(stagingId) }
            throw e
        }
    }

    private suspend fun swapStaging(projectId: Long, stagingId: Long, raw: LongArray) {
        db.withWriteTransaction {
            writeChunks(stagingId, raw)
            dao.clearIndex(projectId)
            dao.promoteIndex(stagingId, projectId)
            dao.clearIndex(stagingId)
        }
    }

    private suspend fun scanNative(path: String, shouldAbort: () -> Boolean): LongArray? = coroutineScope {
        val token = nextToken.incrementAndGet()
        FfmpegBridge.nativeSetIndexCancel(token, false)
        val scan = async(Dispatchers.IO) { FfmpegBridge.nativeFrameIndexCancellable(path, token) }
        val watcher = launch {
            while (isActive && !shouldAbort()) delay(ABORT_POLL_MS)
            if (isActive) FfmpegBridge.nativeSetIndexCancel(token, true)
        }
        try {
            val result = scan.await()
            if (shouldAbort()) throw CancellationException()
            result
        } catch (e: CancellationException) {
            FfmpegBridge.nativeSetIndexCancel(token, true)
            withContext(NonCancellable) { scan.join() }
            throw e
        } finally {
            watcher.cancel()
            FfmpegBridge.nativeReleaseIndexCancel(token)
        }
    }

    private suspend fun writeChunks(stagingId: Long, raw: LongArray) {
        val batch = ArrayList<VideoIndexEntity>(CHUNK)
        var i = 0
        var sample = 0
        while (i < raw.size) {
            batch += VideoIndexEntity(
                projectId = stagingId,
                sampleIndex = sample++,
                ptsUs = raw[i],
                isKeyframe = raw[i + 1] != 0L,
                nearestKeyframeIndex = raw[i + 2].toInt(),
                nearestKeyframePtsUs = raw[i + 3]
            )
            i += 4
            if (batch.size >= CHUNK) {
                dao.insertAll(batch)
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) dao.insertAll(batch)
    }
}
