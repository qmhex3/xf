package com.frameout.app.onboarding

import com.frameout.core.util.SmoothScale
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.frameout.app.R
import com.frameout.designsystem.a11y.LocalSystemA11y
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object OnboardingClips {
    const val HERO = "onboarding/hero.mp4"
    const val FORMATS = "onboarding/formats.mp4"
    const val PICK = "onboarding/pick.mp4"
    const val READY = "onboarding/ready.mp4"
}

internal class ClipHandle(val path: String, val player: ExoPlayer, val phase: State<Float>)

@Composable
internal fun rememberClip(path: String): ClipHandle? {
    val asset = rememberClipAsset(path) ?: return null
    val player = rememberClipPlayer(asset)
    val phase = rememberClipPhase(player)
    return remember(player) { ClipHandle(asset, player, phase) }
}

@Composable
internal fun rememberFirstClip(vararg paths: String): String? {
    val context = LocalContext.current
    return remember(paths.toList()) { paths.firstOrNull { clipExists(context, it) } }
}

private fun clipExists(context: Context, path: String): Boolean {
    val folder = path.substringBeforeLast('/')
    val name = path.substringAfterLast('/')
    return try {
        context.assets.list(folder)?.contains(name) == true
    } catch (e: Exception) {
        false
    }
}

@Composable
private fun rememberClipAsset(path: String): String? {
    val context = LocalContext.current
    return remember(path) { if (clipExists(context, path)) path else null }
}

@Composable
private fun rememberClipPlayer(path: String): ExoPlayer {
    val context = LocalContext.current
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val player = remember(path) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setMediaItem(MediaItem.fromUri("asset:///$path"))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = !reduceMotion
            prepare()
        }
    }
    val owner = LocalActivity.current as? LifecycleOwner
    DisposableEffect(player, owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_START -> if (!reduceMotion) player.play()
                else -> Unit
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose {
            owner?.lifecycle?.removeObserver(observer)
            player.release()
        }
    }
    return player
}

@Composable
private fun rememberClipPhase(player: ExoPlayer): State<Float> {
    val phase = remember(player) { mutableFloatStateOf(0f) }
    LaunchedEffect(player) {
        while (true) {
            withFrameNanos { }
            val duration = player.duration
            if (duration != C.TIME_UNSET && duration > 0L) {
                phase.floatValue = (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            }
        }
    }
    return phase
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
internal fun ClipSurface(exo: ExoPlayer, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.player_texture, null) as PlayerView).apply {
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                player = exo
            }
        },
        modifier = modifier,
        update = { view -> view.player = exo },
        onRelease = { view -> view.player = null }
    )
}

@Composable
internal fun rememberClipFrames(path: String?, fractions: List<Float>, width: Int, height: Int): List<ImageBitmap?> {
    val context = LocalContext.current.applicationContext
    val frames by produceState(List<ImageBitmap?>(fractions.size) { null }, path, fractions, width, height) {
        if (path != null) value = withContext(Dispatchers.IO) { grabFrames(context, path, fractions, width, height) }
    }
    return frames
}

private fun grabFrames(context: Context, path: String, fractions: List<Float>, width: Int, height: Int): List<ImageBitmap?> {
    val retriever = MediaMetadataRetriever()
    return try {
        context.assets.openFd(path).use { fd -> retriever.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
        val durationUs = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) * 1000L
        fractions.map { f ->
            retriever.getFrameAtTime((durationUs * f).toLong(), MediaMetadataRetriever.OPTION_CLOSEST)
                ?.let { cropTo(it, width, height).asImageBitmap() }
        }
    } catch (e: Exception) {
        fractions.map { null }
    } finally {
        retriever.release()
    }
}

private fun cropTo(src: Bitmap, width: Int, height: Int): Bitmap {
    val target = width.toFloat() / height
    val ratio = src.width.toFloat() / src.height
    val cw = if (ratio > target) (src.height * target).toInt() else src.width
    val ch = if (ratio > target) src.height else (src.width / target).toInt()
    val cropped = Bitmap.createBitmap(src, (src.width - cw) / 2, (src.height - ch) / 2, cw.coerceAtLeast(1), ch.coerceAtLeast(1))
    return SmoothScale.scaled(cropped, width, height, true)
}
