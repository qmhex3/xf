package com.frameout.app.onboarding

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

internal object ClipStills {
    private const val DIR = "onboarding/frames"
    private val bitmaps = ConcurrentHashMap<String, ImageBitmap>()
    @Volatile private var names: Set<String>? = null

    private fun listing(context: Context): Set<String> =
        names ?: (try { context.assets.list(DIR)?.toSet() } catch (e: Exception) { null } ?: emptySet()).also { names = it }

    private fun clipName(path: String) = path.substringAfterLast('/').removeSuffix(".mp4")

    private fun fileName(path: String, f: Float, w: Int, h: Int) = "${clipName(path)}_${(f * 1000f).roundToInt()}_${w}x$h.webp"

    fun exists(context: Context, path: String): Boolean {
        val prefix = clipName(path) + "_"
        return listing(context).any { it.startsWith(prefix) }
    }

    fun cached(path: String?, fractions: List<Float>, w: Int, h: Int): List<ImageBitmap?>? {
        if (path == null) return null
        val list = fractions.map { bitmaps[fileName(path, it, w, h)] }
        return if (list.all { it != null }) list else null
    }

    suspend fun load(context: Context, path: String, fractions: List<Float>, w: Int, h: Int): List<ImageBitmap?>? {
        val files = fractions.map { fileName(path, it, w, h) }
        return withContext(Dispatchers.IO) {
            val available = listing(context)
            if (files.any { it !in available }) return@withContext null
            files.map { decode(context, it) }.takeIf { list -> list.all { it != null } }
        }
    }

    private fun decode(context: Context, name: String): ImageBitmap? {
        bitmaps[name]?.let { return it }
        val bmp = try {
            context.assets.open("$DIR/$name").use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        } ?: return null
        return bmp.asImageBitmap().also { bitmaps[name] = it }
    }

    suspend fun preloadAll(context: Context) = withContext(Dispatchers.IO) {
        listing(context).forEach { decode(context, it) }
    }
}

internal object OnboardingPreload {
    suspend fun run(context: Context) {
        try { ClipStills.preloadAll(context.applicationContext) } catch (e: Exception) { }
    }
}
