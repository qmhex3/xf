package com.frameout.core.media

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

internal object ProbeStore {
    private const val LIMIT = 60_000
    private const val FLUSH_EVERY = 32
    private const val DIR = "probe_verdicts"
    private val spaces = ConcurrentHashMap<String, Space>()

    fun get(context: Context, space: String, key: String): Boolean? = space(context, space).get(hash(key))

    fun put(context: Context, space: String, key: String, value: Boolean) = space(context, space).put(hash(key), value)

    fun flushAll() = spaces.values.forEach { it.flush() }

    internal fun hash(key: String): Long {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        var value = 0L
        for (i in 0 until 8) value = (value shl 8) or (digest[i].toLong() and 0xFF)
        return value
    }

    private fun space(context: Context, name: String): Space = spaces.computeIfAbsent(name) {
        val dir = File(context.applicationContext.filesDir, DIR).also { it.mkdirs() }
        Space(File(dir, "$name.bin"))
    }

    internal class Space(private val file: File) {
        private val map = object : LinkedHashMap<Long, Boolean>(1024, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Boolean>?): Boolean = size > LIMIT
        }
        private var out: DataOutputStream? = null
        private var records = 0
        private var pending = 0
        private var loaded = false

        @Synchronized
        fun get(key: Long): Boolean? {
            load()
            return map[key]
        }

        @Synchronized
        fun put(key: Long, value: Boolean) {
            load()
            if (map[key] == value) return
            map[key] = value
            append(key, value)
            if (records > LIMIT * 2) rewrite()
        }

        @Synchronized
        fun flush() {
            runCatching { out?.flush() }
            pending = 0
        }

        private fun load() {
            if (loaded) return
            loaded = true
            if (!file.isFile) return
            runCatching {
                DataInputStream(BufferedInputStream(FileInputStream(file), 64 * 1024)).use { input ->
                    while (true) {
                        val key = try { input.readLong() } catch (_: EOFException) { break }
                        val value = try { input.readByte() } catch (_: EOFException) { break }
                        map[key] = value.toInt() != 0
                        records++
                    }
                }
            }
            if (records > map.size * 2) rewrite()
        }

        private fun append(key: Long, value: Boolean) {
            runCatching {
                val stream = out ?: DataOutputStream(BufferedOutputStream(FileOutputStream(file, true), 8 * 1024)).also { out = it }
                stream.writeLong(key)
                stream.writeByte(if (value) 1 else 0)
                records++
                if (++pending >= FLUSH_EVERY) {
                    stream.flush()
                    pending = 0
                }
            }.onFailure {
                runCatching { out?.close() }
                out = null
            }
        }

        private fun rewrite() {
            runCatching { out?.close() }
            out = null
            pending = 0
            runCatching {
                val temp = File(file.parentFile, "${file.name}.tmp")
                DataOutputStream(BufferedOutputStream(FileOutputStream(temp), 64 * 1024)).use { stream ->
                    map.forEach { (key, value) ->
                        stream.writeLong(key)
                        stream.writeByte(if (value) 1 else 0)
                    }
                }
                if (temp.renameTo(file)) records = map.size else temp.delete()
            }
        }
    }
}
