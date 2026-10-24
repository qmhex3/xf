package com.frameout.core.quality.content

import android.graphics.Bitmap
import com.frameout.core.quality.FaceQuality
import com.frameout.core.quality.FaceScanner
import com.frameout.core.quality.SmartZoom
import com.frameout.core.quality.ZoomScan
import com.frameout.core.quality.await
import com.frameout.core.quality.ArabicOcr
import com.frameout.core.util.DetectorHealth
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object ContentFilter {
    private const val SAMPLE_SIDE = 768
    private const val MIN_SIDE = 48
    private const val LOOSE_TILE = 0.7f
    private val gate = Semaphore(1)
    private val faceKinds = setOf(ContentDetector.Face, ContentDetector.Smile, ContentDetector.Group)

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(ContentMatcher.LABEL_FLOOR).build())
    }
    private val text by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val codes by lazy {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build())
    }

    suspend fun matches(source: Bitmap, spec: ContentSpec): Boolean = gate.withPermit {
        val tags = ContentCatalog.selected(spec.tags)
        if (tags.isEmpty() || source.isRecycled || source.width < MIN_SIDE || source.height < MIN_SIDE) return@withPermit true
        val argb = prepared(source) ?: return@withPermit true
        try {
            evaluate(argb, spec, tags)
        } finally {
            if (argb !== source) argb.recycle()
        }
    }

    private suspend fun evaluate(argb: Bitmap, spec: ContentSpec, tags: List<ContentTag>): Boolean {
        val image = InputImage.fromBitmap(argb, 0)
        val hits = HashSet<String>()
        val labelTags = tags.filter { it.detector == ContentDetector.Label }
        if (labelTags.isNotEmpty()) {
            val scores = labelScores(argb, image, spec, labelTags) ?: return true
            hits += ContentMatcher.labelHits(labelTags, scores, ContentMatcher.threshold(spec.strict))
            if (done(spec, hits)) return ContentMatcher.satisfied(spec, hits)
        }
        val faceTags = tags.filter { it.detector in faceKinds }
        if (faceTags.isNotEmpty()) {
            val found = detect("content-face") { FaceScanner.detect(argb) } ?: return true
            val smiling = found.boxes.count { it.smile >= FaceQuality.SMILE_THRESHOLD }
            hits += ContentMatcher.faceHits(faceTags, found.boxes.size, smiling)
            if (done(spec, hits)) return ContentMatcher.satisfied(spec, hits)
        }
        return evaluateReaders(argb, image, spec, tags, hits)
    }

    private suspend fun evaluateReaders(argb: Bitmap, image: InputImage, spec: ContentSpec, tags: List<ContentTag>, hits: MutableSet<String>): Boolean {
        tags.firstOrNull { it.detector == ContentDetector.Text }?.let { tag ->
            val read = detect("content-text") { text.process(image).await().text + " " + ArabicOcr.plainText(argb) } ?: return true
            if (ContentMatcher.textHit(read)) hits += tag.id
        }
        tags.firstOrNull { it.detector == ContentDetector.Code }?.let { tag ->
            val found = detect("content-code") { codes.process(image).await().size } ?: return true
            if (found > 0) hits += tag.id
        }
        return ContentMatcher.satisfied(spec, hits)
    }

    private fun MutableMap<String, Float>.best(key: String, value: Float) {
        if (value > (this[key] ?: 0f)) this[key] = value
    }

    private fun done(spec: ContentSpec, hits: Set<String>): Boolean = !spec.all && ContentMatcher.satisfied(spec, hits)

    private fun labelsEnough(spec: ContentSpec, labelTags: List<ContentTag>, hits: Set<String>): Boolean =
        if (spec.all) hits.containsAll(labelTags.map { it.id }) else hits.isNotEmpty()

    private fun mergeTiles(whole: Map<String, Float>, tiles: Map<String, List<Float>>, single: Float): Map<String, Float> {
        val scores = HashMap<String, Float>()
        (whole.keys + tiles.keys).forEach { text ->
            SmartZoom.vote(whole[text], tiles[text].orEmpty(), single)?.let { scores[text] = it }
        }
        return scores
    }

    private suspend fun labelScores(argb: Bitmap, image: InputImage, spec: ContentSpec, labelTags: List<ContentTag>): Map<String, Float>? {
        val whole = HashMap<String, Float>()
        detect("content-labels") { labeler.process(image).await() }?.forEach { whole.best(it.text, it.confidence) } ?: return null
        if (spec.strict == ContentSpec.STRICT_STRICT) return whole
        val min = ContentMatcher.threshold(spec.strict)
        if (labelsEnough(spec, labelTags, ContentMatcher.labelHits(labelTags, whole, min))) return whole
        val wanted = labelTags.flatMapTo(HashSet()) { it.labels }
        val single = if (spec.strict == ContentSpec.STRICT_LOOSE) LOOSE_TILE else SmartZoom.TILE_STRONG
        val crops = SmartZoom.grid(argb.width, argb.height).mapNotNull { ZoomScan.crop(argb, it, SmartZoom.TILE_TARGET, SmartZoom.MAX_UPSCALE) }
        if (crops.isEmpty()) return whole
        try {
            val perTile = coroutineScope {
                crops.map { crop -> async { detect("content-tile") { labeler.process(InputImage.fromBitmap(crop.bitmap, 0)).await() } } }.awaitAll()
            }
            val tiles = HashMap<String, MutableList<Float>>()
            perTile.forEach { labels ->
                val seen = HashMap<String, Float>()
                labels?.forEach { if (it.text in wanted) seen.best(it.text, it.confidence) }
                seen.forEach { (text, score) -> tiles.getOrPut(text) { ArrayList() } += score }
            }
            if (tiles.isEmpty()) return whole
            return mergeTiles(whole, tiles, single)
        } finally {
            crops.forEach { it.release() }
        }
    }

    private fun prepared(source: Bitmap): Bitmap? {
        val scale = SAMPLE_SIDE.toFloat() / maxOf(source.width, source.height)
        val small = if (scale >= 1f) source else Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(MIN_SIDE), (source.height * scale).toInt().coerceAtLeast(MIN_SIDE), true)
        if (small.config == Bitmap.Config.ARGB_8888) return small
        val copy = small.copy(Bitmap.Config.ARGB_8888, false)
        if (small !== source) small.recycle()
        return copy
    }

    private suspend fun <T> detect(name: String, block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectorHealth.failed(name, e)
        null
    }
}
