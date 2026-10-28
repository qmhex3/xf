package com.frameout.app.billing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.R
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private const val MAX_STAGGER = 6

private class ProItem(val art: Int, val name: String, val line: String, val feature: ProFeature?, val from: Color, val to: Color)

private fun featureItem(art: Int, feature: ProFeature, from: Long, to: Long): ProItem =
    ProItem(art, ProStrings.featureName(feature), ProStrings.featureLine(feature), feature, Color(from), Color(to))

private fun proItems(): List<ProItem> = listOf(
    featureItem(R.drawable.pro_art_smart, ProFeature.SmartExtract, 0xFFFF5F3D, 0xFFFFB02E),
    featureItem(R.drawable.pro_art_filters, ProFeature.Filters, 0xFF12B886, 0xFF84CC16),
    featureItem(R.drawable.pro_art_shapes, ProFeature.Shapes, 0xFF0EA5A4, 0xFF22C55E),
    featureItem(R.drawable.pro_art_edit, ProFeature.EditTools, 0xFF3B82F6, 0xFF22D3EE),
    featureItem(R.drawable.pro_art_formats, ProFeature.ExportFormats, 0xFFF43F5E, 0xFFFB923C),
    featureItem(R.drawable.pro_art_export, ProFeature.AdvancedExport, 0xFF8B5CF6, 0xFFEC4899),
    featureItem(R.drawable.pro_art_collage, ProFeature.Collage, 0xFF14B8A6, 0xFF3B82F6),
    featureItem(R.drawable.pro_art_batch, ProFeature.Batch, 0xFFFF6B9A, 0xFFFF9A5C),
    featureItem(R.drawable.pro_art_compare, ProFeature.Compare, 0xFFFF8A3D, 0xFFFFC24D),
    featureItem(R.drawable.pro_art_accents, ProFeature.Accents, 0xFF7C5CFF, 0xFFB06BFF),
    featureItem(R.drawable.pro_art_icons, ProFeature.AppIcons, 0xFF06B6D4, 0xFF6366F1),
    featureItem(R.drawable.pro_art_insights, ProFeature.Insights, 0xFFEAB308, 0xFFF97316),
    ProItem(R.drawable.pro_art_noads, ProStrings.noAdsName(), ProStrings.noAdsLine(), null, Color(0xFF6366F1), Color(0xFFA855F7))
)

@Composable
fun ProFeatureList(motion: SheetMotion, current: ProFeature?, modifier: Modifier = Modifier) {
    val items = proItems()
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ListHeader(items.size, Modifier.revealed(motion.reveal, 2))
        items.forEachIndexed { index, item ->
            ProFeatureRow(item, item.feature != null && item.feature == current, Modifier.revealed(motion.reveal, 2 + minOf(index, MAX_STAGGER)))
        }
    }
}

@Composable
private fun ListHeader(count: Int, modifier: Modifier) {
    val colors = FrameTheme.colors
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(16.dp).background(Brush.verticalGradient(listOf(colors.accentA, colors.accentC)), FrameShapes.pill))
        FrameText(ProStrings.gridTitle(), FrameTheme.typography.titleSmall, colors.onSurface, Modifier.weight(1f).padding(start = 9.dp))
        Box(Modifier.background(colors.primary.copy(alpha = 0.14f), FrameShapes.pill).padding(horizontal = 10.dp, vertical = 3.dp)) {
            FrameText(count.toString(), FrameTheme.typography.label.copy(fontSize = 11.sp), colors.primary)
        }
    }
}

@Composable
private fun ProFeatureRow(item: ProItem, highlight: Boolean, modifier: Modifier) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.md
    val fill = if (highlight) colors.primary.copy(alpha = 0.10f) else colors.onSurface.copy(alpha = 0.05f)
    val edge = if (highlight) colors.primary.copy(alpha = 0.7f) else frameHairline()
    Row(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .background(fill, shape)
            .border(if (highlight) 1.5.dp else 1.dp, edge, shape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtTile(item)
        Column(Modifier.weight(1f)) {
            FrameText(item.name, FrameTheme.typography.titleSmall)
            FrameText(item.line, FrameTheme.typography.bodySmall, colors.onSurfaceVariant, Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ArtTile(item: ProItem) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(48.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(item.from, item.to)))
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape),
        Alignment.Center
    ) {
        Image(painterResource(item.art), null, Modifier.size(32.dp))
    }
}
