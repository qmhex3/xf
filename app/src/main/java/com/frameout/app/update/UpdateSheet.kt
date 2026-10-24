package com.frameout.app.update

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.glass.frameSheetSurface
import com.frameout.designsystem.motion.frameEnter
import com.frameout.designsystem.motion.frameExit
import com.frameout.designsystem.motion.frameFadeIn
import com.frameout.designsystem.motion.frameFadeOut
import com.frameout.designsystem.theme.FrameTheme

private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

@Composable
fun UpdateSheet(
    visible: Boolean,
    info: UpdateInfo,
    onLater: () -> Unit,
    onUpdate: () -> Unit,
    onShowFixes: (() -> Unit)? = null
) {
    BackHandler(enabled = visible, onBack = onLater)
    UpdateScrim(visible, onLater)
    AnimatedVisibility(visible, enter = frameEnter(), exit = frameExit(), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), Alignment.BottomCenter) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .frameSheetSurface(SheetShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .verticalScroll(rememberScrollState())
            ) {
                UpdateHero(info.versionName)
                UpdateBody(info, onLater, onUpdate, onShowFixes, Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun UpdateScrim(visible: Boolean, onClose: () -> Unit) {
    AnimatedVisibility(visible, enter = frameFadeIn(), exit = frameFadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (FrameTheme.colors.isDark) 0.5f else 0.34f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
        )
    }
}
