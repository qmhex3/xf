package com.frameout.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.theme.FramePressIndication
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardBrush
import com.frameout.designsystem.theme.frameCardEdge

enum class DialogTone { Info, Danger, Success, Warning }

class DialogButton(val label: String, val onClick: () -> Unit)

@Composable
fun dialogToneColor(tone: DialogTone): Color {
    val colors = FrameTheme.colors
    return when (tone) {
        DialogTone.Info -> colors.primary
        DialogTone.Danger -> colors.error
        DialogTone.Success -> colors.success
        DialogTone.Warning -> colors.warning
    }
}

@Composable
fun FrameDialog(
    onDismiss: () -> Unit,
    tone: DialogTone,
    title: String,
    primary: DialogButton,
    modifier: Modifier = Modifier,
    body: String? = null,
    secondary: DialogButton? = null,
    icon: Int? = null,
    badge: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val accent = dialogToneColor(tone)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalIndication provides FramePressIndication) {
            DialogCard(accent, modifier) {
                if (badge != null) badge() else if (icon != null) ToneBadge(icon, accent)
                DialogTexts(title, body)
                content?.invoke(this)
                DialogButtons(accent, primary, secondary)
            }
        }
    }
}

@Composable
private fun DialogCard(accent: Color, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) enter.animateTo(1f, spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow))
    }
    val dark = FrameTheme.colors.isDark
    Column(
        modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .graphicsLayer {
                val p = enter.value
                scaleX = 0.9f + 0.1f * p
                scaleY = 0.9f + 0.1f * p
                alpha = p.coerceIn(0f, 1f)
            }
            .clip(FrameShapes.xxl)
            .background(frameCardBrush(accent, if (dark) 0.10f else 0.06f))
            .drawBehind {
                val glow = Brush.radialGradient(listOf(accent.copy(alpha = if (dark) 0.26f else 0.16f), Color.Transparent), Offset(size.width / 2f, 0f), size.width * 0.8f)
                drawRect(glow)
            }
            .border(1.dp, frameCardEdge(accent, if (dark) 0.42f else 0.30f), FrameShapes.xxl)
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun ToneBadge(icon: Int, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(76.dp)
            .drawBehind {
                drawCircle(accent.copy(alpha = 0.10f))
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.06f))), radius = size.minDimension * 0.36f)
                drawCircle(accent.copy(alpha = 0.40f), radius = size.minDimension * 0.36f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            },
        Alignment.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(28.dp), colorFilter = ColorFilter.tint(accent))
    }
}

@Composable
private fun DialogTexts(title: String, body: String?) {
    FrameText(title, style = FrameTheme.typography.title, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
    if (!body.isNullOrBlank()) {
        FrameText(
            body,
            style = FrameTheme.typography.body,
            color = FrameTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
private fun DialogButtons(accent: Color, primary: DialogButton, secondary: DialogButton?) {
    Column(Modifier.fillMaxWidth().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionPill(primary.label, accent, true, primary.onClick)
        secondary?.let { ActionPill(it.label, accent, false, it.onClick) }
    }
}

@Composable
private fun ActionPill(label: String, accent: Color, filled: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val fill = if (filled) Modifier.background(Brush.horizontalGradient(listOf(lerp(accent, Color.White, 0.10f), lerp(accent, colors.accentB, 0.30f))), FrameShapes.pill)
    else Modifier.background(colors.onSurface.copy(alpha = 0.07f), FrameShapes.pill)
    Box(
        Modifier.fillMaxWidth().heightIn(min = if (filled) 52.dp else 48.dp).clip(FrameShapes.pill).then(fill).frameClickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        Alignment.Center
    ) {
        FrameText(label, style = FrameTheme.typography.label, color = if (filled) readableOn(accent) else colors.onSurface, textAlign = TextAlign.Center)
    }
}

private fun readableOn(background: Color): Color {
    val dark = Color(0xFF0E1110)
    return if (ratio(background, dark) >= ratio(background, Color.White)) dark else Color.White
}

private fun ratio(a: Color, b: Color): Float {
    val x = a.luminance() + 0.05f
    val y = b.luminance() + 0.05f
    return if (x > y) x / y else y / x
}
