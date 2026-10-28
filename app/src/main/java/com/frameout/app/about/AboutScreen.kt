package com.frameout.app.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
        val colors = FrameTheme.colors
        val scroll = rememberScrollState()
        val reduce = LocalSystemA11y.current.reduceMotion
        val body = remember { Animatable(if (reduce) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (reduce) return@LaunchedEffect
            delay(480)
            body.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        }
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            FrameTopBar(
                title = ModeStrings.about(),
                onBack = onBack,
                backContentDescription = FrameStrings.back(),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            AboutHero(scroll)
            AboutCards(body)
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
private fun AboutCards(body: Animatable<Float, AnimationVector1D>) {
    val colors = FrameTheme.colors
    Column(
        Modifier.fillMaxWidth().graphicsLayer {
            alpha = body.value
            translationY = (1f - body.value) * 24.dp.toPx()
        },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AboutCard(ModeStrings.aboutOfflineTitle(), ModeStrings.aboutOfflineBody(), FrameIconsExtra.Storage, colors.accentA)
        if (com.frameout.app.BuildConfig.PLAY_BILLING) {
            AboutCard(ModeStrings.aboutProTitle(), ModeStrings.aboutProBody(), FrameIconsExtra.Sparkle, colors.accentB)
        }
        ConnectGroup()
        AboutCard(ModeStrings.aboutLicensesTitle(), ModeStrings.aboutLicensesBody(), FrameIcons.Info, colors.accentC)
        Column { CodecPackLicenseSection() }
        com.frameout.app.remote.PromoSlot(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

private class ConnectRow(val icon: Int, val tint: Color, val title: String, val body: String, val trailing: Int, val onClick: () -> Unit)

@Composable
private fun ConnectGroup() {
    val colors = FrameTheme.colors
    val context = LocalContext.current
    val rows = buildList {
        if (AboutConfig.hasYoutube) {
            add(ConnectRow(FrameIconsExtra.Youtube, YoutubeRed, ModeStrings.aboutYoutubeTitle(), ModeStrings.aboutYoutubeBody(), FrameIconsExtra.Link) { AboutLinks.openYoutube(context) })
        }
        if (AboutConfig.hasEmail) {
            add(ConnectRow(FrameIconsExtra.Mail, colors.primary, ModeStrings.aboutEmailTitle(), ModeStrings.aboutEmailBody(), FrameIconsExtra.Chevron) { AboutLinks.writeEmail(context) })
        }
        add(ConnectRow(FrameIcons.StarFilled, colors.warning, RatingStrings.rowTitle(), RatingStrings.rowBody(), FrameIconsExtra.Chevron) { StoreLauncher.openStore(context) })
    }
    Column {
        FrameSectionTitle(ModeStrings.aboutConnectTitle(), Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
        GlassSurface(shape = FrameShapes.xl, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                rows.forEachIndexed { i, row ->
                    if (i > 0) {
                        Box(Modifier.fillMaxWidth().padding(start = 74.dp).height(1.dp).background(colors.outline.copy(alpha = 0.5f)))
                    }
                    ConnectRowView(row)
                }
            }
        }
    }
}

@Composable
private fun ConnectRowView(row: ConnectRow) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().frameClickable(onClick = row.onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBadge(row.icon, row.tint)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FrameText(row.title, style = FrameTheme.typography.titleSmall)
            FrameText(row.body, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Image(painterResource(row.trailing), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
    }
}

@Composable
internal fun IconBadge(icon: Int, tint: Color) {
    Box(
        Modifier.size(48.dp).clip(FrameShapes.sm).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(26.dp), colorFilter = ColorFilter.tint(tint))
    }
}

@Composable
internal fun AboutCard(title: String, body: String, icon: Int? = null, tint: Color = Color.Unspecified) {
    GlassSurface(shape = FrameShapes.xl, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (icon != null) IconBadge(icon, if (tint == Color.Unspecified) FrameTheme.colors.primary else tint)
                FrameText(title, style = FrameTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
            FrameText(body, style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant)
        }
    }
}
