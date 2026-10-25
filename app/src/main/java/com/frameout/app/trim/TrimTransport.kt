package com.frameout.app.trim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIconsExtra
import kotlinx.coroutines.launch

@Composable
internal fun TransportRow(env: TrimEnv) {
    val step = { forward: Boolean, n: Int -> env.scope.launch { env.controller.stepFrame(forward, n) }; Unit }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally), Alignment.CenterVertically) {
                TransportKey(FrameStrings.stepBack(), onStep = { step(false, it) }) { TransportIcon(FrameIconsExtra.StepBack) }
                PlayButton(env.player.isPlaying, FrameStrings.play(), FrameStrings.pause(), env.controller::toggle)
                TransportKey(FrameStrings.stepForward(), onStep = { step(true, it) }) { TransportIcon(FrameIconsExtra.StepForward) }
            }
        }
    }
}
