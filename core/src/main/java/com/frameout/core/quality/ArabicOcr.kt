package com.frameout.core.quality

import android.content.Context
import android.graphics.Bitmap
import com.frameout.core.util.DetectorHealth
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object ArabicOcr {
    private const val LANG = "ara"
    private const val ASSET = "tessdata/ara.traineddata"
    private const val MAX_SIDE = 1600
    private const val MIN_CONFIDENCE = 40
    private val lock = Mutex()
    private var appContext: Context? = null
    private var api: TessBaseAPI? = null
    private var broken = false

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun read(source: Bitmap): List<ScannedWord> {
        if (broken || source.isRecycled) return emptyList()
        return lock.withLock {
            withContext(Dispatchers.Default) {
                try {
                    recognise(source)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    broken = true
                    DetectorHealth.failed("arabic-ocr", e)
                    emptyList()
                }
            }
        }
    }

    suspend fun plainText(source: Bitmap): String = read(source).joinToString(" ") { it.text }

    private fun recognise(source: Bitmap): List<ScannedWord> {
        val engine = engine() ?: return emptyList()
        val work = scaled(source)
        try {
            engine.setImage(work)
            engine.getUTF8Text()
            return words(engine, work.width, work.height)
        } finally {
            engine.clear()
            if (work !== source) work.recycle()
        }
    }

    private fun words(engine: TessBaseAPI, w: Int, h: Int): List<ScannedWord> {
        val it = engine.resultIterator ?: return emptyList()
        val out = ArrayList<ScannedWord>()
        try {
            it.begin()
            var line = 0
            do {
                val text = it.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)?.trim().orEmpty()
                val rect = it.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                if (text.isNotEmpty() && it.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD) >= MIN_CONFIDENCE) {
                    val box = NormBox(
                        (rect.left.toFloat() / w).coerceIn(0f, 1f), (rect.top.toFloat() / h).coerceIn(0f, 1f),
                        (rect.right.toFloat() / w).coerceIn(0f, 1f), (rect.bottom.toFloat() / h).coerceIn(0f, 1f)
                    )
                    if (box.r > box.l && box.b > box.t) out += ScannedWord(text, box, line)
                }
                if (it.isAtFinalElement(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE, TessBaseAPI.PageIteratorLevel.RIL_WORD)) line++
            } while (it.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
        } finally {
            it.delete()
        }
        return out
    }

    private fun scaled(source: Bitmap): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= MAX_SIDE) return source
        val k = MAX_SIDE.toFloat() / longest
        return Bitmap.createScaledBitmap(source, (source.width * k).toInt().coerceAtLeast(1), (source.height * k).toInt().coerceAtLeast(1), true)
    }

    private fun engine(): TessBaseAPI? {
        api?.let { return it }
        val context = appContext ?: return null
        val root = File(context.filesDir, "ocr")
        val data = File(root, "tessdata/$LANG.traineddata")
        if (!stage(context, data)) {
            broken = true
            return null
        }
        val created = TessBaseAPI()
        if (!created.init(root.absolutePath, LANG, TessBaseAPI.OEM_LSTM_ONLY)) {
            created.recycle()
            broken = true
            return null
        }
        created.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
        api = created
        return created
    }

    private fun stage(context: Context, target: File): Boolean = try {
        val updated = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        if (!target.exists() || target.length() == 0L || target.lastModified() < updated) {
            target.parentFile?.mkdirs()
            context.assets.open(ASSET).use { input -> target.outputStream().use { input.copyTo(it) } }
        }
        true
    } catch (e: Exception) {
        DetectorHealth.failed("arabic-ocr-model", e)
        false
    }
}
