package com.frameout.core.util

import android.content.Context
import com.frameout.core.db.AppDatabase
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.storage.StorageManager
import com.frameout.core.edit.EditHistoryRepository

class AppContainer(context: Context, proEnforced: Boolean = false, legacyInstall: Boolean = false) {
    val database: AppDatabase = AppDatabase.get(context)
    val prefs: AppPrefs = AppPrefs(context)
    val storage: StorageManager = StorageManager(context)
    val projects = database.projectDao()
    val frames = database.frameDao()
    val savedImages = database.savedImageDao()
    val frameEdits = database.frameEditDao()
    val editHistory = EditHistoryRepository(database)
    val videoIndex = database.videoIndexDao()
    val frameTags = database.frameTagDao()
    val sharpest = database.sharpestDao()
    val device = DeviceCapability(context).also { DeviceLimits.install(it) }
    val bitmaps = ThumbCache.pool
    val rating = com.frameout.core.rating.RatingTracker.init(context)
    val entitlements = com.frameout.core.pro.Entitlements(context, proEnforced, legacyInstall)
    val notice = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    init {
        MemoryTrim.register(context)
    }
}
