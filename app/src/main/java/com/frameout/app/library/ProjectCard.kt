package com.frameout.app.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.frameout.engine.work.LiveExtractProgress
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.thumb.rememberThumb
import com.frameout.codecpack.MediaFailure
import com.frameout.core.db.ProjectEntity
import com.frameout.core.db.progressTotal
import com.frameout.core.util.FailureMarker
import com.frameout.core.util.FailureStrings
import com.frameout.core.util.FrameA11y
import com.frameout.core.util.ThumbCache
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameCombinedClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardBrush
import com.frameout.designsystem.theme.frameCardEdge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProjectCard(
    project: ProjectEntity,
    selected: Boolean,
    onOpen: () -> Unit,
    onLongClick: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = FrameTheme.colors
    val context = LocalContext.current
    val thumb = rememberThumb(project.thumbnailPath, { ThumbCache.peek(project.thumbnailPath) }, { ThumbCache.load(project.thumbnailPath) }).value
    val failure = produceState<MediaFailure?>(null, project.status, project.updatedAt) {
        value = if (project.status == "Failed") withContext(Dispatchers.IO) { FailureMarker.read(context, project.id) } else null
    }.value
    val specs = produceState(peekSpecs(project), project.id, project.sourceUri) { value = loadSpecs(context, project) }.value
    val live = isLive(project.status)
    val pulse = livePulse(live)
    val shape = FrameShapes.xl
    val border = when {
        selected -> Modifier.border(2.dp, colors.primary, shape)
        live -> Modifier.border(1.5.dp, frameCardEdge(colors.primary, 0.70f), shape)
        else -> Modifier.border(1.dp, frameCardEdge(), shape)
    }
    val fill = if (live) frameCardBrush(colors.primary, 0.16f) else frameCardBrush()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .frameCombinedClickable(onOpen, onLongClick, FrameA11y.select())
            .semantics { this.selected = selected }
            .then(if (live) Modifier.frameGlow(colors.primary, { pulse?.value ?: 0.16f }, 12.dp, { 0.12f }) else Modifier)
            .background(fill, shape)
            .then(border)
    ) {
        Column(Modifier.clip(shape).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProjectThumb(thumb, project, 76.dp)
                ProjectInfo(project, specs, Modifier.weight(1f))
                if (!selected) DeleteAction(onDelete) else Spacer(Modifier.size(48.dp))
            }
            failure?.let { FrameText(FailureStrings.of(it), style = FrameTheme.typography.bodySmall, color = colors.error) }
            CardFooter(project, selected, onResume, onPause)
        }
    }
}

@Composable
private fun livePulse(live: Boolean): State<Float>? {
    val reduce = LocalSystemA11y.current.reduceMotion
    return if (live && !reduce) {
        rememberInfiniteTransition(label = "cardGlow").animateFloat(
            initialValue = 0.10f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "cardGlowValue"
        )
    } else null
}

@Composable
private fun ProjectInfo(project: ProjectEntity, specs: VideoSpecs?, modifier: Modifier) {
    val colors = FrameTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FrameText(project.name, style = FrameTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        specsText(specs)?.let { FrameText(it, style = FrameTheme.typography.monoSmall.copy(fontSize = 10.sp), color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        FrameText(formatDate(project.updatedAt), style = FrameTheme.typography.monoSmall, color = colors.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun CardFooter(project: ProjectEntity, selected: Boolean, onResume: () -> Unit, onPause: () -> Unit) {
    val allLive by LiveExtractProgress.all.collectAsState()
    val live by remember(project.id) { derivedStateOf { allLive[project.id] } }
    val peak = remember(project.id) { floatArrayOf(0f) }
    val progress = cardProgress(project, live).let { shown ->
        if (shown is CardProgress.Value) {
            peak[0] = maxOf(shown.fraction, peak[0])
            CardProgress.Value(peak[0], percentOf(peak[0]))
        } else {
            peak[0] = 0f
            shown
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (progress != CardProgress.None) ProjectProgressBar(progress, Modifier)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusPill(project.status)
            FrameCountInline(counters(project), (progress as? CardProgress.Value)?.percent, com.frameout.core.util.FrameStrings.frameCount(project.extractedCount))
            Box(Modifier.weight(1f))
            if (hasStateAction(project)) {
                Box(Modifier.size(40.dp), Alignment.Center) { if (!selected) StateAction(project, onResume, onPause) }
            }
        }
    }
}

private fun specsText(specs: VideoSpecs?): String? {
    if (specs == null) return null
    val parts = listOfNotNull(specsHeadline(specs), specs.codec, specs.fps?.let(::fpsLabel))
    return parts.takeIf { it.isNotEmpty() }?.joinToString(com.frameout.core.util.FrameStrings.sep())
}

private fun counters(project: ProjectEntity): String =
    if (project.progressTotal > 0 && project.status != "Completed" && project.status != "Indexing") "${project.extractedCount}/${project.progressTotal}" else "${project.extractedCount}"

private fun formatDate(ms: Long) = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(ms))
