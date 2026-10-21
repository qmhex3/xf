package com.frameout.app.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.rating.StoreLauncher
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ModeStrings
import com.frameout.core.util.RatingStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameSectionTitle
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.FrameTopBar
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private val YoutubeRed = Color(0xFFFF3B30)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    PreferredFrameTheme {
        Column(
            Modifier.fillMaxSize().background(FrameTheme.colors.background).statusBarsPadding()
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            FrameTopBar(
                title = ModeStrings.about(),
                onBack = onBack,
                backContentDescription = FrameStrings.back(),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            AboutHero()
            Reveal(1) { ConnectSection() }
            Reveal(2) { AboutCard(ModeStrings.aboutOfflineTitle(), ModeStrings.aboutOfflineBody()) }
            if (com.frameout.app.BuildConfig.PLAY_BILLING) {
                Reveal(3) { AboutCard(ModeStrings.aboutProTitle(), ModeStrings.aboutProBody()) }
            }
            Reveal(4) { AboutCard(ModeStrings.aboutLicensesTitle(), ModeStrings.aboutLicensesBody()) }
            Reveal(5) { Column { CodecPackLicenseSection() } }
        }
    }
}

@Composable
private fun ConnectSection() {
    val colors = FrameTheme.colors
    val context = LocalContext.current
    Column {
        FrameSectionTitle(ModeStrings.aboutConnectTitle(), Modifier.padding(horizontal = 4.dp))
        if (AboutConfig.hasYoutube) {
            LinkCard(
                icon = FrameIconsExtra.Youtube,
                tint = YoutubeRed,
                title = ModeStrings.aboutYoutubeTitle(),
                body = ModeStrings.aboutYoutubeBody(),
                trailing = FrameIconsExtra.Link
            ) { AboutLinks.openYoutube(context) }
        }
        if (AboutConfig.hasEmail) {
            LinkCard(
                icon = FrameIconsExtra.Mail,
                tint = colors.primary,
                title = ModeStrings.aboutEmailTitle(),
                body = ModeStrings.aboutEmailBody(),
                trailing = FrameIconsExtra.Chevron
            ) { AboutLinks.writeEmail(context) }
        }
        LinkCard(
            icon = FrameIcons.StarFilled,
            tint = colors.warning,
            title = RatingStrings.rowTitle(),
            body = RatingStrings.rowBody(),
            trailing = FrameIconsExtra.Chevron
        ) { StoreLauncher.openStore(context) }
    }
}

@Composable
private fun Reveal(index: Int, content: @Composable () -> Unit) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay(220L + index * 90L)
        progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow))
    }
    Box(
        Modifier.fillMaxWidth().graphicsLayer {
            alpha = progress.value.coerceIn(0f, 1f)
            translationY = (1f - progress.value) * 32.dp.toPx()
        }
    ) { content() }
}

@Composable
private fun LinkCard(icon: Int, tint: Color, title: String, body: String, trailing: Int, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    GlassSurface(shape = FrameShapes.md, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth().frameClickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(48.dp).clip(FrameShapes.sm).background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(icon), null, Modifier.size(26.dp), colorFilter = ColorFilter.tint(tint))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FrameText(title, style = FrameTheme.typography.titleSmall)
                FrameText(body, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Image(painterResource(trailing), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
        }
    }
}

@Composable
internal fun AboutCard(title: String, body: String) {
    GlassSurface(shape = FrameShapes.md, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            FrameText(title, style = FrameTheme.typography.titleSmall)
            FrameText(body, style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
