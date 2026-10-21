package com.frameout.app.trim

import android.view.LayoutInflater
import androidx.compose.foundation.Image
import androidx.media3.common.util.UnstableApi
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.frameout.app.FrameOutApp
import com.frameout.designsystem.icons.FrameIconsExtra
import androidx.compose.foundation.background
import com.frameout.designsystem.theme.frameControl
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.positionChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.frameout.app.R
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.frameout.engine.player.PlayerController
import com.frameout.engine.player.PlayerUiState
import androidx.compose.foundation.layout.BoxScope

@Composable
fun PlayerSurface(
    controller: PlayerController,
    aspect: Float = 0f,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onTap: (() -> Unit)? = null,
    showMute: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    var zoom by remember { mutableStateOf(ZoomState()) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    var resetJob by remember { mutableStateOf<Job?>(null) }
    val animateReset: () -> Unit = {
        resetJob?.cancel()
        val from = zoom
        resetJob = scope.launch {
            animate(0f, 1f, animationSpec = tween(RESET_MS, easing = FastOutSlowInEasing)) { t, _ -> zoom = from.relaxed(t) }
        }
    }
    val ui by controller.state.collectAsState()
    val clipFrame by controller.frame.collectAsState()
    val appPrefs = (LocalContext.current.applicationContext as FrameOutApp).container.prefs
    LaunchedEffect(controller) { controller.setMuted(appPrefs.playerMuted.value) }
    val ratio = if (aspect > 0f) aspect.coerceIn(0.4f, 3f) else 16f / 9f
    Box(modifier, Alignment.Center) {
        Box(
            Modifier
                .aspectRatio(ratio)
                .clip(FrameShapes.md)
                .background(Color.Black)
                .onSizeChanged { box = it }
                .pointerInput(Unit) { detectTapGestures(onTap = { onTap?.invoke() }) }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        pinchZoomGesture({ zoom }, { Size(box.width.toFloat(), box.height.toFloat()) }) {
                            resetJob?.cancel()
                            zoom = it
                        }
                    }
                }
        ) {
            PlayerLayers(controller, ui, clipFrame, zoom, showMute, animateReset)
            overlay()
        }
    }
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
private fun BoxScope.PlayerLayers(controller: PlayerController, ui: PlayerUiState, clipFrame: android.graphics.Bitmap?, zoom: ZoomState, showMute: Boolean, onResetZoom: () -> Unit) {
    val zoomLayer: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
        scaleX = zoom.scale
        scaleY = zoom.scale
        translationX = zoom.offset.x
        translationY = zoom.offset.y
    }
    if (ui.animated) {
        clipFrame?.let { bitmap ->
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(image, contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer(block = zoomLayer), contentScale = ContentScale.Fit)
        }
    } else {
        AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.player_texture, null) as PlayerView).apply {
                    player = controller.attach()
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            modifier = Modifier.fillMaxSize().graphicsLayer(block = zoomLayer)
        )
    }
    if (ui.buffering && !ui.animated) com.frameout.designsystem.components.FrameSpinner(Modifier.align(Alignment.Center), size = 40.dp)
    AnimatedVisibility(
        visible = zoom.zoomed,
        modifier = Modifier.align(Alignment.TopEnd),
        enter = fadeIn(tween(160)) + scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 0.85f, transformOrigin = TransformOrigin(1f, 0f)),
        exit = fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.9f, transformOrigin = TransformOrigin(1f, 0f))
    ) { ZoomBadge(zoom.scale, onResetZoom) }
}

@Composable
internal fun MuteKey(muted: Boolean, controller: PlayerController, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val prefs = (LocalContext.current.applicationContext as FrameOutApp).container.prefs
    val label = if (muted) FrameStrings.unmute() else FrameStrings.mute()
    Box(
        modifier
            .size(48.dp)
            .frameControl(CircleShape, selected = muted, glowAlpha = if (muted) 0.5f else 0f)
            .clip(CircleShape)
            .frameClickable(onClick = {
                prefs.setPlayerMuted(!muted)
                controller.setMuted(!muted)
            })
            .semantics { contentDescription = label },
        Alignment.Center
    ) {
        Image(
            painterResource(if (muted) FrameIconsExtra.VolumeOff else FrameIconsExtra.Volume),
            null,
            Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(if (muted) colors.primary else colors.onSurfaceVariant)
        )
    }
}

private const val RESET_MS = 280

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.pinchZoomGesture(
    current: () -> ZoomState,
    size: () -> Size,
    apply: (ZoomState) -> Unit
) {
    awaitFirstDown(requireUnconsumed = false)
    val slop = viewConfiguration.touchSlop
    var zoomMotion = 0f
    var panMotion = 0f
    var engaged = false
    do {
        val event = awaitPointerEvent()
        if (event.changes.any { it.isConsumed }) return
        val fingers = event.changes.count { it.pressed }
        if (fingers >= 2 || current().zoomed) {
            val zoomChange = event.calculateZoom()
            val pan = event.calculatePan()
            if (!engaged) {
                zoomMotion += abs(1f - zoomChange) * event.calculateCentroidSize(useCurrent = false)
                panMotion += pan.getDistance()
                if (fingers >= 2 && zoomMotion > slop || panMotion > slop) engaged = true
            }
            if (engaged) {
                if (zoomChange != 1f || pan != Offset.Zero) {
                    apply(current().transform(event.calculateCentroid(useCurrent = false), pan, zoomChange, size()))
                }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } else {
            engaged = false
            zoomMotion = 0f
            panMotion = 0f
        }
    } while (event.changes.any { it.pressed })
}

@Composable
private fun ZoomBadge(scale: Float, onReset: () -> Unit) {
    val label = String.format(Locale.US, "%.1f\u00d7", scale)
    val description = FrameStrings.zoomReset(label)
    val shape = FrameShapes.pill
    Row(
        Modifier
            .padding(10.dp)
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(Color(0xB8101216), shape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .frameClickable(onClickLabel = description, onClick = onReset)
            .semantics { contentDescription = description }
            .padding(start = 10.dp, end = 4.dp),
        Arrangement.spacedBy(6.dp),
        Alignment.CenterVertically
    ) {
        Image(painterResource(FrameIconsExtra.Zoom), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.9f)))
        FrameText(label, style = FrameTheme.typography.label, color = Color.White)
        Box(Modifier.size(28.dp).background(Color.White.copy(alpha = 0.14f), CircleShape), Alignment.Center) {
            Image(painterResource(FrameIconsExtra.Fit), null, Modifier.size(15.dp), colorFilter = ColorFilter.tint(Color.White))
        }
    }
}
