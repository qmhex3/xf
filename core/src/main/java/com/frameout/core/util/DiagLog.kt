package com.frameout.core.util

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object DiagLog {
    private const val MEMORY_LINES = 200
    private const val EXPORT_FILE_BYTES = 50_000
    private const val CRASH_WAIT_MS = 1_500L
    private const val EXPORT_WAIT_MS = 3_000L

    private val memory = ArrayDeque<String>(MEMORY_LINES)
    private val pending = LinkedBlockingQueue<String>()
    private val scheduled = AtomicBoolean(false)
    private val executor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { task -> Thread(task, "diag-log").apply { isDaemon = true } }
    }
    @Volatile private var sink: DiagSink? = null
    private var handlerInstalled = false

    fun init(context: Context) {
        val dir = File(context.filesDir, "diag")
        dir.mkdirs()
        attach(DiagFileSink(File(dir, "frameout-diag.log")))
        installCrashHandler()
    }

    fun attach(target: DiagSink) {
        sink = target
    }

    fun reset() {
        sink = null
        pending.clear()
        synchronized(memory) { memory.clear() }
    }

    fun append(tag: String, msg: String) {
        val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "$stamp [$tag] ${DiagRedactor.redact(msg)}"
        synchronized(memory) {
            memory.addLast(line)
            while (memory.size > MEMORY_LINES) memory.removeFirst()
        }
        if (sink != null) {
            pending.offer(line)
            schedule()
        }
    }

    fun flush(timeoutMs: Long = CRASH_WAIT_MS): Boolean {
        if (sink == null) return true
        return try {
            executor.submit(::writePending).get(timeoutMs, TimeUnit.MILLISECONDS)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun export(context: Context): File {
        flush(EXPORT_WAIT_MS)
        val recent = synchronized(memory) { memory.toList() }
        val stored = sink?.tail(EXPORT_FILE_BYTES).orEmpty()
        val dir = File(context.cacheDir, "diag")
        dir.mkdirs()
        val file = File(dir, "frameout-diag.txt")
        file.writeText(buildString {
            appendLine("Frame out diagnostic")
            appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("sdk=${Build.VERSION.SDK_INT}")
            appendLine("abi=${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("---")
            recent.forEach { appendLine(DiagRedactor.redact(it)) }
            if (stored.isNotEmpty()) {
                appendLine("--- file log ---")
                appendLine(DiagRedactor.redact(stored))
            }
        })
        return file
    }

    private fun schedule() {
        if (!scheduled.compareAndSet(false, true)) return
        try {
            executor.execute(::drain)
        } catch (_: Exception) {
            scheduled.set(false)
        }
    }

    private fun drain() {
        do {
            writePending()
            scheduled.set(false)
        } while (pending.isNotEmpty() && scheduled.compareAndSet(false, true))
    }

    private fun writePending() {
        val batch = ArrayList<String>()
        while (pending.drainTo(batch) > 0) {
            runCatching { sink?.write(batch.toList()) }
            batch.clear()
        }
    }

    @Synchronized
    private fun installCrashHandler() {
        if (handlerInstalled) return
        handlerInstalled = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                append("crash", describe(error))
                flush()
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun describe(error: Throwable): String = runCatching {
        "${error.javaClass.simpleName}: ${error.message}\n${error.stackTraceToString().take(2000)}"
    }.getOrDefault(error.javaClass.name)
}
