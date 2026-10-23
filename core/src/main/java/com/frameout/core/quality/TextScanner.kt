package com.frameout.core.quality

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.frameout.core.util.DetectorHealth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class ScannedWord(val text: String, val box: NormBox, val line: Int)

class TextScanResult(val words: List<ScannedWord>, val failed: List<String>) {
    val allFailed: Boolean get() = failed.size >= TextScanner.DETECTORS
}

object TextScanner {
    private const val MAX_SIDE = 2560
    const val DETECTORS = 2
    private val gate = Semaphore(1)

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val barcodes by lazy {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build())
    }

    suspend fun scan(source: Bitmap): List<ScannedWord> = scanChecked(source).words

    suspend fun scanChecked(source: Bitmap): TextScanResult = gate.withPermit {
        withContext(Dispatchers.Default) {
            try {
                scanOnce(source)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                DetectorHealth.failed("text", e)
                TextScanResult(emptyList(), listOf("text", "barcodes"))
            }
        }
    }

    private suspend fun scanOnce(source: Bitmap): TextScanResult {
        val work = prepare(source) ?: return TextScanResult(emptyList(), emptyList())
        return try {
            val image = InputImage.fromBitmap(work, 0)
            val out = ArrayList<ScannedWord>()
            val failed = ArrayList<String>()
            if (!attempt("text") { readText(image, work, work.width, work.height, out) }) failed += "text"
            if (!attempt("barcodes") { readCodes(image, work.width, work.height, out) }) failed += "barcodes"
            TextScanResult(out, failed)
        } finally {
            work.recycle()
        }
    }

    private suspend fun readText(image: InputImage, work: Bitmap, w: Int, h: Int, out: MutableList<ScannedWord>) {
        var line = 0
        recognizer.process(image).await().textBlocks.forEach { block ->
            block.lines.forEach { l ->
                var any = false
                l.elements.forEach { el ->
                    val box = el.boundingBox?.let { norm(it, w, h) }
                    if (box != null && el.text.isNotBlank()) {
                        out += ScannedWord(el.text, box, line)
                        any = true
                    }
                }
                if (any) line++
            }
        }
        val base = (out.maxOfOrNull { it.line } ?: -1) + 1
        ArabicOcr.read(work).forEach { out += ScannedWord(it.text, it.box, base + it.line) }
    }

    private suspend fun readCodes(image: InputImage, w: Int, h: Int, out: MutableList<ScannedWord>) {
        var line = (out.maxOfOrNull { it.line } ?: -1) + 1
        barcodes.process(image).await().forEach { code ->
            val value = code.rawValue ?: code.displayValue
            val box = code.boundingBox?.let { norm(it, w, h) }
            if (!value.isNullOrBlank() && box != null) out += ScannedWord(value, box, line++)
        }
    }

    private suspend fun attempt(name: String, block: suspend () -> Unit): Boolean = try {
        block()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectorHealth.failed(name, e)
        false
    }

    private fun prepare(source: Bitmap): Bitmap? {
        if (source.isRecycled) return null
        val base = if (source.config == Bitmap.Config.ARGB_8888) source else source.copy(Bitmap.Config.ARGB_8888, false) ?: throw IllegalStateException("bitmap copy failed")
        val longest = maxOf(base.width, base.height)
        if (longest <= MAX_SIDE) return base.copy(Bitmap.Config.ARGB_8888, false)
        val k = MAX_SIDE.toFloat() / longest
        return Bitmap.createScaledBitmap(base, (base.width * k).toInt().coerceAtLeast(1), (base.height * k).toInt().coerceAtLeast(1), true)
    }

    private fun norm(r: android.graphics.Rect, w: Int, h: Int): NormBox? {
        if (w <= 0 || h <= 0) return null
        val box = NormBox(
            (r.left.toFloat() / w).coerceIn(0f, 1f), (r.top.toFloat() / h).coerceIn(0f, 1f),
            (r.right.toFloat() / w).coerceIn(0f, 1f), (r.bottom.toFloat() / h).coerceIn(0f, 1f)
        )
        return box.takeIf { it.r > it.l && it.b > it.t }
    }
}
