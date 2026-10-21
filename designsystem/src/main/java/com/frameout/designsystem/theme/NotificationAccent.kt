package com.frameout.designsystem.theme

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

data class NotificationAccent(val strong: Int, val soft: Int)

fun notificationAccent(accentKey: String, dark: Boolean, context: Context): NotificationAccent {
    val accent = resolveFrameAccent(accentKey, context)
    val strong = (if (dark) accent.darkPrimary else accent.lightPrimary).toArgb()
    val soft = ColorUtils.blendARGB(strong, android.graphics.Color.WHITE, 0.4f)
    return NotificationAccent(strong, soft)
}
