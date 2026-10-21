package com.frameout.app.trim

import com.frameout.designsystem.theme.frameControl
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameSpinner
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.launch

@Composable
internal fun SnapshotButton(env: TrimEnv, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val label = TrimStrings.snapshot()
    val haptics = rememberFrameHaptics()
    val sound = rememberSnapshotSound()
    var capturing by remember { mutableStateOf(false) }
    val click = if (capturing) Modifier else Modifier.frameClickable(onClick = {
        capturing = true
        env.fx.fire()
        sound.play()
        haptics.perform(FrameHaptic.Confirm)
        env.controller.pause()
        env.scope.launch {
            val status = captureFrameToGallery(env.controller, env.saver, env.prefs, env.uri) { env.fx.show(it) }
            if (status.error) haptics.perform(FrameHaptic.Reject)
            env.onStatus(status)
            capturing = false
        }
    })
    Row(
        modifier
            .then(click)
            .height(40.dp)
            .frameControl(FrameShapes.pill, selected = true, glowAlpha = 0.8f)
            .clip(FrameShapes.pill)
            .semantics { contentDescription = label }
            .padding(horizontal = 14.dp),
        Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        Alignment.CenterVertically
    ) {
        if (capturing) FrameSpinner(Modifier.padding(1.dp), size = 18.dp, strokeWidth = 2.dp)
        else Image(painterResource(FrameIconsExtra.Snapshot), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(colors.primary))
        FrameText(label, style = FrameTheme.typography.label, color = colors.primary, maxLines = 1)
    }
}
