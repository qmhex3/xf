package com.frameout.app.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameSpinner
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.fxFloat
import com.frameout.designsystem.motion.fxRise
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill

@Composable
internal fun LibraryLoading(modifier: Modifier = Modifier.fillMaxSize()) {
    val loading = FrameStrings.loading()
    Box(modifier.semantics { contentDescription = loading }, Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FrameSpinner(size = 36.dp)
            FrameText(loading, color = FrameTheme.colors.onSurfaceVariant, style = FrameTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun LibraryEmpty(
    icon: Int,
    title: String,
    hint: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val colors = FrameTheme.colors
    Box(modifier.padding(horizontal = 32.dp), Alignment.Center) {
        Column(Modifier.fxRise(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fxFloat().size(72.dp).background(colors.fieldFill, FrameShapes.lg), Alignment.Center) {
                Image(painterResource(icon), null, Modifier.size(34.dp), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
            }
            Box(Modifier.size(8.dp))
            FrameText(title, style = FrameTheme.typography.titleSmall, textAlign = TextAlign.Center)
            if (hint.isNotEmpty()) FrameText(hint, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                FrameText(
                    actionLabel,
                    style = FrameTheme.typography.label,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(com.frameout.designsystem.theme.FrameGradients.Brand, FrameShapes.pill)
                        .frameClickable(onClick = onAction)
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                )
            }
        }
    }
}
