package com.frameout.app.trim

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.R

internal class SnapshotSound(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private var soundId = 0
    @Volatile private var loaded = false

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (id == soundId && status == 0) loaded = true }
        soundId = pool.load(context.applicationContext, R.raw.snapshot_shutter, 1)
    }

    fun play() {
        if (loaded) pool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        loaded = false
        pool.release()
    }
}

@Composable
internal fun rememberSnapshotSound(): SnapshotSound {
    val context = LocalContext.current
    val sound = remember { SnapshotSound(context) }
    DisposableEffect(sound) { onDispose { sound.release() } }
    return sound
}
