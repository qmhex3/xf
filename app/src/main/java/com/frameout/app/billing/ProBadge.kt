package com.frameout.app.billing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameTheme

private val BadgeShape = RoundedCornerShape(9.dp)

@Composable
fun ProBadge(modifier: Modifier = Modifier, phase: Int = 0, compact: Boolean = false) {
    val shimmer = rememberShimmer(3400, (phase % 5) * 450)
    val ink = proOnBrush()
    Row(
        modifier
            .height(18.dp)
            .clip(BadgeShape)
            .background(proBrush(), BadgeShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.22f), BadgeShape)
            .shimmerSweep(shimmer)
            .padding(start = if (compact) 4.dp else 5.dp, end = if (compact) 4.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(10.dp), colorFilter = ColorFilter.tint(ink))
        if (!compact) {
            FrameText(
                ProStrings.badge(),
                style = FrameTheme.typography.label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                color = ink,
                modifier = Modifier.padding(start = 3.dp),
                maxLines = 1
            )
        }
    }
}
