package com.frameout.designsystem.dock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.frameHoldRepeat
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.motion.LaunchOrigin
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.motion.trackLaunchOrigin
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

@Composable
fun FrameFab(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = FrameTheme.colors.primary,
    contentColor: Color = FrameTheme.colors.onPrimary,
    repeat: Boolean = false
) {
    val colors = FrameTheme.colors
    val haptics = rememberFrameHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.93f else 1f, frameSpring(), label = "fabPress")
    val primary = containerColor == colors.primary
    val tint = if (primary && contentColor == colors.onPrimary) Color.White else contentColor
    Box(
        modifier = modifier
            .size(FrameFabSize)
            .trackLaunchOrigin()
            .scale(press)
            .then(if (primary) Modifier.frameGlow(colors.primary, { FabRestGlow }, FabGlowSpread, { 0.5f }) else Modifier)
            .clip(FrameFabShape)
            .background(if (primary) FrameGradients.Fab else SolidColor(containerColor))
            .then(if (primary) Modifier.background(FabSheen) else Modifier)
            .border(1.dp, if (primary) FabRim else frameHairline(), FrameFabShape)
            .then(
                if (repeat) Modifier.frameHoldRepeat(onClick)
                else Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        haptics.perform(FrameHaptic.Tap)
                        LaunchOrigin.mark(colors.primary)
                        onClick()
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(26.dp),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}
