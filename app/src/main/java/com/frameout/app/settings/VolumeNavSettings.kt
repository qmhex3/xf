package com.frameout.app.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
internal fun VolumeNavSettings(prefs: AppPrefs) {
    val enabled by prefs.volumeNav.collectAsState()
    ToggleRow(FrameIconsExtra.Volume, FrameStrings.volumeNavTitle(), FrameStrings.volumeNavHint(), enabled) { prefs.setVolumeNav(it) }
}
