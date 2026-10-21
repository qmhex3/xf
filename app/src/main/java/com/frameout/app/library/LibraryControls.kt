package com.frameout.app.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline

private val SlotHeight = 48.dp
private val SlotWidth = 100.dp

@Composable
fun LibraryControls(
    query: String,
    onQuery: (String) -> Unit,
    hint: String,
    onImport: () -> Unit,
    sortLabel: String,
    sortActive: Boolean,
    menu: @Composable (dismiss: () -> Unit) -> Unit
) {
    val colors = FrameTheme.colors
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchField(
            query, onQuery, Modifier.weight(1f), hint,
            trailing = {
                Box {
                    SortTrigger(
                        description = FrameStrings.sortAndFilter() + ": " + sortLabel,
                        active = sortActive || open,
                        onClick = { open = !open }
                    )
                    if (open) {
                        val gap = with(LocalDensity.current) { 52.dp.roundToPx() }
                        Popup(
                            alignment = Alignment.TopEnd,
                            offset = IntOffset(0, gap),
                            onDismissRequest = { open = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                            MenuSurface { menu { open = false } }
                        }
                    }
                }
            }
        )
        ControlSlot(
            icon = FrameIconsExtra.Import,
            text = FrameStrings.importProject(),
            description = FrameStrings.importProject(),
            tone = colors.primary,
            highlighted = true,
            strong = true,
            drift = 1,
            modifier = Modifier.padding(start = 8.dp),
            onClick = onImport
        )
    }
}

@Composable
private fun SortTrigger(description: String, active: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (active) colors.primary else colors.onSurfaceVariant, frameSpring(), label = "sortTint")
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (active) colors.primary.copy(alpha = 0.14f) else Color.Transparent)
            .frameClickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        Alignment.Center
    ) {
        Image(painterResource(FrameIconsExtra.Sort), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(tint))
    }
}

@Composable
private fun ControlSlot(
    icon: Int,
    text: String,
    description: String,
    tone: Color,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    drift: Int = 0,
    strong: Boolean = false
) {
    val colors = FrameTheme.colors
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val haptics = rememberFrameHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.94f else 1f, frameSpring(), label = "slotPress")
    val travel by animateFloatAsState(if (pressed && !reduceMotion) drift * 4f else 0f, frameSpring(), label = "slotTravel")
    val fill by animateColorAsState(if (highlighted) tone.copy(alpha = if (strong && colors.isDark) 0.20f else 0.14f) else colors.fieldFill, frameSpring(), label = "slotFill")
    val edge by animateColorAsState(if (highlighted) tone.copy(alpha = if (strong) 0.50f else 0.36f) else frameHairline(), frameSpring(), label = "slotEdge")
    val tint by animateColorAsState(if (highlighted) tone else colors.onSurfaceVariant, frameSpring(), label = "slotTint")
    Box(
        modifier
            .size(SlotWidth, SlotHeight)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .clip(FrameShapes.pill)
            .background(fill)
            .border(1.dp, edge, FrameShapes.pill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.perform(FrameHaptic.Tap)
                    onClick()
                }
            )
            .semantics { contentDescription = description },
        Alignment.Center
    ) {
        AnimatedContent(
            targetState = icon to text,
            transitionSpec = {
                if (reduceMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    (fadeIn(tween(200, delayMillis = 60)) + scaleIn(tween(240), initialScale = 0.7f)) togetherWith
                        (fadeOut(tween(110)) + scaleOut(tween(150), targetScale = 0.7f))
                }
            },
            label = "slotContent"
        ) { (currentIcon, currentText) ->
            Row(
                Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    painterResource(currentIcon), null,
                    Modifier.size(20.dp).graphicsLayer { translationY = travel.dp.toPx() },
                    colorFilter = ColorFilter.tint(tint)
                )
                FrameText(currentText, style = FrameTheme.typography.label, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}
