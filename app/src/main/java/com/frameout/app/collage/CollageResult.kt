package com.frameout.app.collage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frameout.app.trim.SavedCapture
import com.frameout.app.trim.open
import com.frameout.app.trim.share
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.liveMessage
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun SaveSuccessOverlay(result: SavedCapture, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = FrameTheme.colors
    Box(
        modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(Modifier.padding(horizontal = 28.dp).fillMaxWidth(), FrameShapes.lg, glow = colors.success) {
            Column(Modifier.padding(20.dp), Arrangement.spacedBy(16.dp), Alignment.CenterHorizontally) {
                Image(painterResource(FrameIcons.Check), null, Modifier.size(40.dp), colorFilter = ColorFilter.tint(colors.success))
                FrameText(
                    FrameStrings.savedOk(),
                    style = FrameTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                    modifier = Modifier.liveMessage()
                )
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                    ResultButton(FrameStrings.share(), false, Modifier.weight(1f)) { result.share(context) }
                    ResultButton(FrameStrings.openSaved(), false, Modifier.weight(1f)) { result.open(context) }
                }
                ResultButton(FrameStrings.done(), true, Modifier.fillMaxWidth(), onDone)
            }
        }
    }
}

@Composable
private fun ResultButton(label: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        modifier
            .height(48.dp)
            .clip(FrameShapes.pill)
            .background(if (primary) colors.primary else colors.primaryContainer)
            .frameClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        FrameText(
            label,
            style = FrameTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
            color = if (primary) colors.onPrimary else colors.primary,
            maxLines = 1
        )
    }
}
