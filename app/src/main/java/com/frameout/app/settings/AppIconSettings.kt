package com.frameout.app.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.frameout.designsystem.theme.FrameTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.frameout.app.icon.AppIconSwitcher
import com.frameout.app.icon.IconRaster
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
internal fun AppIconSettings(prefs: AppPrefs, follows: Boolean, accent: String, theme: String) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val key = remember(follows, accent, theme, systemDark) {
        if (follows) AppIconSwitcher.keyFor(accent, theme, systemDark) else "indigo_dark"
    }
    val preview = remember(key) { IconRaster.render(context, IconRaster.iconRes(context, key), 512)?.asImageBitmap() }

    ToggleRow(FrameIconsExtra.Album, FrameStrings.iconFollowsTheme(), FrameStrings.iconFollowsThemeHint(), follows, com.frameout.app.billing.rememberProGate().locked(false)) {
        com.frameout.core.pro.ProAccess.source?.gate(com.frameout.core.pro.ProFeature.AppIcons, !it, "follow") { prefs.setIconFollowsTheme(it) } ?: prefs.setIconFollowsTheme(it)
    }
    preview?.let {
        Row(
            Modifier.fillMaxWidth().padding(start = 64.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                it,
                null,
                Modifier.size(56.dp).clip(CircleShape).border(1.dp, FrameTheme.colors.outline, CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}
