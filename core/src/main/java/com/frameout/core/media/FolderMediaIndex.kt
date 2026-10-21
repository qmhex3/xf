package com.frameout.core.media

import android.content.Context
import android.media.MediaScannerConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

object FolderMediaIndex {
    private const val TTL_MS = 60_000L
    private const val PROMOTE_LIMIT = 100
    private const val PREWARM_DELAY_MS = 2_000L

    private class Cached(val entries: List<FolderEntry>, val at: Long, val access: Int)

    private val lock = ReentrantLock()
    private val promoted = HashSet<String>()
    private val refreshing = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    @Volatile private var cached: Cached? = null

    val changes: SharedFlow<Unit> = updates.asSharedFlow()

    suspend fun load(context: Context): List<FolderEntry> = runInterruptible(Dispatchers.IO) { snapshot(context) }

    suspend fun loadSafely(context: Context): List<FolderEntry> = try {
        load(context)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

    fun prewarm(context: Context) {
        val app = context.applicationContext
        scope.launch {
            delay(PREWARM_DELAY_MS)
            runCatching { snapshot(app) }
        }
    }

    fun invalidate() {
        cached = cached?.let { Cached(it.entries, 0L, it.access) }
    }

    fun snapshot(context: Context): List<FolderEntry> {
        val app = context.applicationContext
        val access = accessOf(app)
        val current = cached
        if (current != null && current.access == access) {
            if (isStale(current)) refreshInBackground(app)
            return current.entries
        }
        return rebuild(app, access)
    }

    private fun accessOf(context: Context): Int =
        (if (PermissionHelper.hasFullVideoAccess(context)) 1 else 0) or (if (PermissionHelper.hasFullImageAccess(context)) 2 else 0)

    private fun isStale(entry: Cached): Boolean = System.currentTimeMillis() - entry.at >= TTL_MS

    private fun rebuild(context: Context, access: Int): List<FolderEntry> {
        lock.lockInterruptibly()
        try {
            cached?.takeIf { it.access == access && !isStale(it) }?.let { return it.entries }
            val built = build(context)
            val previous = cached
            cached = Cached(built, System.currentTimeMillis(), access)
            if (previous != null && previous.entries != built) updates.tryEmit(Unit)
            return built
        } finally {
            lock.unlock()
        }
    }

    private fun refreshInBackground(context: Context) {
        if (!refreshing.compareAndSet(false, true)) return
        scope.launch {
            try {
                runCatching { rebuild(context, accessOf(context)) }
            } finally {
                refreshing.set(false)
            }
        }
    }

    private fun build(context: Context): List<FolderEntry> {
        val videos = PermissionHelper.hasFullVideoAccess(context)
        val images = PermissionHelper.hasFullImageAccess(context)
        if (!videos && !images) return emptyList()
        val scan = FolderMediaScanner.scan(FolderScanRoots.resolve(context), videos, images)
        val indexed = MediaStorePaths.indexedAmong(context, scan.entries.map { it.path }) ?: return emptyList()
        val missing = scan.entries.filter { it.path !in indexed }
        promote(context, missing.filter { !it.readable })
        return withIds(missing.filter { it.readable })
    }

    fun withIds(entries: List<FolderEntry>): List<FolderEntry> {
        val used = HashSet<Long>()
        val numbered = entries.sortedBy { it.path }.map { entry ->
            var id = -(fnv64(entry.path) ushr 2) - 1L
            while (!used.add(id)) id--
            entry.copy(id = id)
        }
        return numbered.sortedWith(compareByDescending<FolderEntry> { it.dateSec }.thenByDescending { it.id })
    }

    private fun fnv64(text: String): Long {
        var hash = -3750763034362895579L
        for (ch in text) hash = (hash xor ch.code.toLong()) * 1099511628211L
        return hash
    }

    private fun promote(context: Context, entries: List<FolderEntry>) {
        val fresh = entries.filter { promoted.add(it.path) }.take(PROMOTE_LIMIT)
        if (fresh.isEmpty()) return
        runCatching {
            MediaScannerConnection.scanFile(context, fresh.map { it.path }.toTypedArray(), fresh.map { it.mime }.toTypedArray(), null)
        }
    }
}
