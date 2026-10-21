package com.frameout.designsystem.dock

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp

internal val FrameFabSize = 60.dp

internal val FrameFabShape = CircleShape

internal val FabGlowSpread = 10.dp
internal const val FabRestGlow = 0.16f

internal val FabSheen: Brush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color.Black.copy(alpha = 0.10f))
)

internal val FabRim: Color = Color.White.copy(alpha = 0.20f)
