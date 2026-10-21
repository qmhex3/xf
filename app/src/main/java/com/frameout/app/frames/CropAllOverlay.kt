package com.frameout.app.frames

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.frameout.app.viewer.CropOverlay
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.theme.FrameColors
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun CropAllOverlay(
    crop: CropAllState,
    onChange: (Float, Float, Float, Float) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    val colors = FrameTheme.colors
    val image = remember(crop.bitmap) { crop.bitmap.asImageBitmap() }
    Box(
        Modifier
            .fillMaxSize()
            .background(FrameColors.ViewerBlack)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        FrameText(
            FrameStrings.cropAllHint(crop.count),
            style = FrameTheme.typography.label,
            color = colors.onSurface,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp)
        )
        Box(Modifier.fillMaxSize().padding(vertical = 96.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.aspectRatio(crop.bitmap.width.toFloat() / crop.bitmap.height)) {
                Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                CropOverlay(crop.l, crop.t, crop.r, crop.b, crop.bitmap.width.toFloat() / crop.bitmap.height, onChange)
            }
        }
        GlassSurface(
            shape = FrameShapes.pill,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FrameText(FrameStrings.cancel(), style = FrameTheme.typography.label, color = colors.onSurfaceVariant, modifier = Modifier.frameClickable(onClick = onCancel))
                    FrameText(FrameStrings.cropReset(), style = FrameTheme.typography.label, color = colors.onSurfaceVariant, modifier = Modifier.frameClickable(onClick = onReset))
                    FrameText(FrameStrings.done(), style = FrameTheme.typography.label, color = colors.primary, modifier = Modifier.frameClickable(onClick = onApply))
                }
            }
        }
    }
}
