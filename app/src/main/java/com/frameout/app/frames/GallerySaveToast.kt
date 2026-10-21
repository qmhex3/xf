package com.frameout.app.frames

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameIslandToast
import com.frameout.designsystem.components.IslandAction
import com.frameout.designsystem.components.islandToastState
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
internal fun GallerySaveToast(progress: GallerySaveProgress?, onCancel: () -> Unit, modifier: Modifier) {
    val cancel = FrameStrings.cancel()
    val island = remember(progress?.done, progress?.total, cancel) {
        progress?.let {
            islandToastState(
                FrameStrings.gallerySavingProgress(it.done, it.total),
                busy = true,
                actions = listOf(IslandAction(cancel, FrameIconsExtra.Cancel, onClick = onCancel))
            )
        }
    }
    FrameIslandToast(island, modifier)
}
