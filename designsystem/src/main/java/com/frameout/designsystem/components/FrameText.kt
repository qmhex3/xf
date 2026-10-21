package com.frameout.designsystem.components

import com.frameout.designsystem.a11y.screenHeading
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun FrameText(
    text: String,
    style: TextStyle = FrameTheme.typography.body,
    color: Color = FrameTheme.colors.onSurface,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    BasicText(
        text = text,
        style = style.copy(color = color, textAlign = textAlign ?: style.textAlign),
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
        onTextLayout = onTextLayout
    )
}

@Composable
fun FrameSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    FrameText(
        text = text,
        style = FrameTheme.typography.titleSmall,
        color = FrameTheme.colors.onSurfaceVariant,
        modifier = modifier.padding(vertical = 8.dp).screenHeading()
    )
}
