package com.frameout.core.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {
    fun mediaPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            Build.VERSION.SDK_INT >= 33 -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            Build.VERSION.SDK_INT >= 29 -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    fun hasMediaAccess(context: Context): Boolean =
        canReadVideos(context) || canReadImages(context)

    fun isPartialMediaAccess(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 34 &&
            granted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
            (!granted(context, Manifest.permission.READ_MEDIA_VIDEO) || !granted(context, Manifest.permission.READ_MEDIA_IMAGES))

    fun canReadVideos(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= 34 -> granted(context, Manifest.permission.READ_MEDIA_VIDEO) || partial(context)
        Build.VERSION.SDK_INT >= 33 -> granted(context, Manifest.permission.READ_MEDIA_VIDEO)
        else -> granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun canReadImages(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= 34 -> granted(context, Manifest.permission.READ_MEDIA_IMAGES) || partial(context)
        Build.VERSION.SDK_INT >= 33 -> granted(context, Manifest.permission.READ_MEDIA_IMAGES)
        else -> granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasFullVideoAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) granted(context, Manifest.permission.READ_MEDIA_VIDEO)
        else granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)

    fun hasFullImageAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) granted(context, Manifest.permission.READ_MEDIA_IMAGES)
        else granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun partial(context: Context): Boolean = Build.VERSION.SDK_INT >= 34 &&
        granted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

    private fun granted(context: Context, perm: String): Boolean {
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}
