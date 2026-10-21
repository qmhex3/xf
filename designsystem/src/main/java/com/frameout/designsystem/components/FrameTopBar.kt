package com.frameout.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun FrameTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "",
    actionIcon: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        onBack?.let {
            TopBarIconButton(FrameIcons.Back, backContentDescription, it, Modifier.align(Alignment.CenterStart))
        }
        FrameText(title, style = FrameTheme.typography.title, modifier = Modifier.align(Alignment.Center))
        if (actionIcon != null && onAction != null) {
            TopBarIconButton(actionIcon, actionLabel ?: title, onAction, Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
fun TopBarIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(FrameTheme.colors.primary),
        modifier = modifier
            .size(48.dp)
            .frameClickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(12.dp)
    )
}

