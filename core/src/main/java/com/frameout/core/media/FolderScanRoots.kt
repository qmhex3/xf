package com.frameout.core.media

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.core.content.ContextCompat
import java.io.File

internal object FolderScanRoots {
    fun resolve(context: Context): List<File> {
        val candidates = listOf(Environment.getExternalStorageDirectory()) + volumes(context)
        val unique = LinkedHashMap<String, File>()
        candidates.forEach { dir ->
            val canonical = runCatching { dir.canonicalPath }.getOrNull() ?: return@forEach
            if (dir.isDirectory && dir.canRead()) unique.putIfAbsent(canonical, dir)
        }
        return unique.values.toList()
    }

    private fun volumes(context: Context): List<File> = runCatching {
        if (Build.VERSION.SDK_INT >= 30) {
            val manager = context.getSystemService(StorageManager::class.java)
            manager.storageVolumes.filter { it.state == Environment.MEDIA_MOUNTED }.mapNotNull { it.directory }
        } else {
            ContextCompat.getExternalFilesDirs(context, null).filterNotNull().mapNotNull(::volumeRoot)
        }
    }.getOrDefault(emptyList())

    private fun volumeRoot(appDir: File): File? {
        val path = appDir.absolutePath
        val cut = path.indexOf("/Android/data")
        return if (cut > 0) File(path.substring(0, cut)) else null
    }
}
