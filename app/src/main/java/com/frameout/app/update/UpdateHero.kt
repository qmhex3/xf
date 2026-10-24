package com.frameout.app.update

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.UpdateStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private val GraphiteHeroBase = Color(0xFF1C1C1C)
private val GraphiteHeroGlowA = Color(0xFF3A3A3A)
private val GraphiteHeroGlowB = Color(0xFF2A2A2A)

@Composable
internal fun UpdateHero(versionName: String, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val base = remember(colors.accentA, colors.neutral) {
        if (colors.neutral) GraphiteHeroBase else lerp(colors.accentA, Color.Black, 0.34f)
    }
    val glowA = if (colors.neutral) GraphiteHeroGlowA else colors.accentA
    val glowB = if (colors.neutral) GraphiteHeroGlowB else colors.accentB
    Box(
        modifier
            .fillMaxWidth()
            .height(176.dp)
            .drawBehind {
                drawRect(base)
                drawRect(Brush.radialGradient(listOf(glowA, Color.Transparent), Offset(size.width * 0.15f, 0f), size.width * 0.9f))
                drawRect(Brush.radialGradient(listOf(glowB, Color.Transparent), Offset(size.width * 0.9f, size.height * 0.1f), size.width * 0.9f))
            }
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(width = 40.dp, height = 4.dp)
                .background(Color.White.copy(alpha = 0.55f), FrameShapes.pill)
        )
        GhostCard(-12f, Modifier.align(Alignment.Center).absoluteOffset(x = (-62).dp, y = 11.dp))
        GhostCard(12f, Modifier.align(Alignment.Center).absoluteOffset(x = 62.dp, y = 11.dp))
        HeroCard(base, Modifier.align(Alignment.Center).absoluteOffset(y = 5.dp))
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HeroPill(UpdateStrings.badgeNew(), Color.Black.copy(alpha = 0.28f))
            HeroPill("v$versionName", Color.White.copy(alpha = 0.22f))
        }
    }
}

@Composable
private fun HeroPill(text: String, fill: Color) {
    Box(Modifier.background(fill, FrameShapes.pill).padding(horizontal = 11.dp, vertical = 5.dp)) {
        FrameText(
            text,
            style = FrameTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun GhostCard(rotation: Float, modifier: Modifier) {
    val shape = FrameShapes.md
    Column(
        modifier
            .size(width = 78.dp, height = 104.dp)
            .graphicsLayer { rotationZ = rotation }
            .shadow(10.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.28f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.12f))))
            .border(0.75.dp, Color.White.copy(alpha = 0.3f), shape),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        DotRow()
        DotRow()
    }
}

@Composable
private fun DotRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(5) {
            Box(Modifier.size(width = 7.dp, height = 5.dp).background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun HeroCard(coin: Color, modifier: Modifier) {
    val shape = FrameShapes.xl
    Box(
        modifier
            .size(width = 100.dp, height = 130.dp)
            .shadow(14.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.75f)))),
        Alignment.Center
    ) {
        Box(Modifier.size(52.dp).background(coin, FrameShapes.pill), Alignment.Center) {
            Image(painterResource(FrameIconsExtra.Download), null, Modifier.size(26.dp), colorFilter = ColorFilter.tint(Color.White))
        }
    }
}
