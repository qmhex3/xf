package com.frameout.app.library

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun LibraryBrand(modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val style = FrameTheme.typography.display.copy(fontSize = 24.sp, lineHeight = 30.sp)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            FrameText("Frame", style = style)
            FrameText(" out", style = style, color = colors.primary)
        }
    }
}
