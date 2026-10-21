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
import com.frameout.designsystem.glass.framePopupSurface
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

@Composable
internal fun MenuSurface(content: @Composable () -> Unit) {
    val colors = FrameTheme.colors
    Column(
        Modifier
            .width(272.dp)
            .heightIn(max = 460.dp)
            .framePopupSurface(FrameShapes.xl)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) { content() }
}

@Composable
internal fun MenuTitle(text: String) {
    FrameText(
        text,
        style = FrameTheme.typography.label,
        color = FrameTheme.colors.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
    )
}

@Composable
internal fun MenuOption(label: String, selected: Boolean, icon: Int? = null, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .frameClickable(role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 6.dp)
            .clip(FrameShapes.sm)
            .background(if (selected) colors.primary.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon != null) Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(if (selected) colors.primary else colors.onSurfaceVariant))
        FrameText(
            label,
            style = FrameTheme.typography.body,
            color = if (selected) colors.primary else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) Image(painterResource(FrameIcons.Check), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.primary))
    }
}

@Composable
internal fun LibrarySort.label(): String = when (this) {
    LibrarySort.Newest -> FrameStrings.sortNewest()
    LibrarySort.Oldest -> FrameStrings.sortOldest()
    LibrarySort.Name -> FrameStrings.sortName()
    LibrarySort.Largest -> FrameStrings.sortLargest()
}

@Composable
internal fun SortOptions(options: List<LibrarySort>, selected: LibrarySort, onSelect: (LibrarySort) -> Unit) {
    MenuTitle(FrameStrings.sortBy())
    options.forEach { MenuOption(it.label(), it == selected) { onSelect(it) } }
}

@Composable
internal fun AlbumOptions(state: LibraryState, onAlbum: (String?) -> Unit) {
    if (state.albums.size < 2) return
    MenuTitle(FrameStrings.libraryAlbum())
    MenuOption(FrameStrings.libraryAll(), state.selectedAlbum == null) { onAlbum(null) }
    state.albums.forEach { row ->
        val name = row.album.ifBlank { FrameStrings.appName() }
        MenuOption("$name  ${row.total}", state.selectedAlbum == row.album) { onAlbum(row.album) }
    }
}
