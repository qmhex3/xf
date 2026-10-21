package com.frameout.app.trim

import com.frameout.designsystem.glass.frameSheetSurface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.frameout.designsystem.a11y.frameClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.TopBarIconButton
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private val SheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
internal fun BoxScope.TrimSheetHost(visible: Boolean, title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (!visible) return
    BackHandler(onBack = onClose)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.82f).dp
    Box(
        Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.55f)).pointerInput(Unit) { detectTapGestures { onClose() } },
        Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .frameSheetSurface(SheetTop)
                .pointerInput(Unit) { detectTapGestures { } }
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
            Arrangement.spacedBy(16.dp)
        ) {
            SheetHandle()
            SheetTitle(title, onClose)
            Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(14.dp), content = content)
        }
    }
}

@Composable
private fun ColumnScope.SheetHandle() {
    Box(
        Modifier
            .align(Alignment.CenterHorizontally)
            .size(width = 40.dp, height = 4.dp)
            .background(FrameTheme.colors.primary.copy(alpha = 0.45f), FrameShapes.pill)
    )
}

@Composable
private fun SheetTitle(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        FrameText(title, style = FrameTheme.typography.title, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(FrameTheme.colors.container).frameClickable(onClick = onClose).semantics { contentDescription = FrameStrings.close() },
            Alignment.Center
        ) {
            Image(painterResource(FrameIcons.Close), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.onSurface))
        }
    }
}
