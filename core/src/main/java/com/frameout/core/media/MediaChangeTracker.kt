package com.frameout.core.media

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object MediaChangeTracker {
    private val counter = AtomicInteger()
    private val started = AtomicBoolean(false)
    private var observer: ContentObserver? = null

    val version: Int get() = counter.get()

    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val created = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                counter.incrementAndGet()
            }
        }
        observer = created
        context.applicationContext.contentResolver.registerContentObserver(MediaStore.Files.getContentUri("external"), true, created)
    }
}
