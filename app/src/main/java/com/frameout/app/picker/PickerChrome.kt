package com.frameout.app.picker

import androidx.compose.ui.draw.clip
import com.frameout.designsystem.theme.frameControl
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.PickerStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.TopBarIconButton
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import com.frameout.designsystem.theme.fieldFill

@Composable
fun PickerTopBar(onClose: () -> Unit, onSaf: () -> Unit, onManageAccess: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopBarIconButton(FrameIcons.Close, FrameStrings.close(), onClose)
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            FrameText(FrameStrings.selectVideo(), style = FrameTheme.typography.title, modifier = Modifier.screenHeading())
            if (onManageAccess != null) LimitedAccessHint()
        }
        FilesButton(onSaf)
        if (onManageAccess != null) {
            TopBarIconButton(FrameIconsExtra.Adjust, FrameStrings.manageAccess(), onManageAccess)
        } else {
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun LimitedAccessHint() {
    val colors = FrameTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(colors.success, CircleShape))
        FrameText(
            PickerStrings.limitedAccess(),
            style = FrameTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun FilesButton(onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .frameControl(FrameShapes.pill)
            .clip(FrameShapes.pill)
            .frameClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painterResource(FrameIconsExtra.Folder),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.primary),
            modifier = Modifier.size(18.dp)
        )
        FrameText(FrameStrings.files(), style = FrameTheme.typography.label)
    }
}
