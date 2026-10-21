package com.frameout.app.settings

import com.frameout.designsystem.glass.frameSheetSurface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.frameEnter
import com.frameout.designsystem.motion.frameExit
import com.frameout.designsystem.motion.frameFadeIn
import com.frameout.designsystem.motion.frameFadeOut
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
internal fun BottomSheetFrame(visible: Boolean, onClose: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = FrameTheme.colors
    BackHandler(enabled = visible, onBack = onClose)
    SheetScrim(visible, onClose)
    AnimatedVisibility(visible, enter = frameEnter(), exit = frameExit(), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), Alignment.BottomCenter) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .frameSheetSurface(SheetShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(top = 10.dp, bottom = 12.dp)
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 4.dp).background(colors.primary.copy(alpha = 0.45f), FrameShapes.pill))
                FrameText(
                    title,
                    style = FrameTheme.typography.title,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).screenHeading()
                )
                content()
            }
        }
    }
}

@Composable
private fun SheetScrim(visible: Boolean, onClose: () -> Unit) {
    AnimatedVisibility(visible, enter = frameFadeIn(), exit = frameFadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (FrameTheme.colors.isDark) 0.5f else 0.34f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
        )
    }
}
