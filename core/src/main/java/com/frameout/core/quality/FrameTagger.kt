package com.frameout.core.quality

import android.graphics.Bitmap
import com.frameout.core.util.DetectorHealth
import com.frameout.core.util.DeviceLimits
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object FrameTagger {
    const val VERSION = 4
    private const val MIN_SIDE = 48
    private const val LABEL_CONFIDENCE = 0.6f
    private const val OBJECT_CONFIDENCE = 0.4f
    private const val MAX_REGIONS = 5
    private const val MIN_REGION = 28
    private const val ZOOM_REGION_TARGET = 256
    private const val ZOOM_REGION_MAX = 4f
    private const val ZOOMED_LABEL_CONFIDENCE = 0.7f
    private const val TILE_OBJECT_CONFIDENCE = 0.6f
    private const val OBJECT_DUPLICATE_IOU = 0.4f
    private const val REGION_PAD = 0.08f
    private const val REGION_LABELS = 2
    private const val BODY_PAD = 0.06f
    private val gate = Semaphore(1)

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(LABEL_CONFIDENCE).build())
    }
    private val objects by lazy {
        ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()
        )
    }
    private val text by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val barcodes by lazy {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build())
    }
    private val pose by lazy {
        PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE).build())
    }

    const val DETECTORS = 6

    suspend fun tag(key: String, source: Bitmap): List<TagHit> = tagDetailed(key, source).hits

    suspend fun tagDetailed(key: String, source: Bitmap): TagOutcome = gate.withPermit {
        if (source.isRecycled || source.width < MIN_SIDE || source.height < MIN_SIDE) return@withPermit TagOutcome(emptyList())
        val argb = if (source.config == Bitmap.Config.ARGB_8888) source else source.copy(Bitmap.Config.ARGB_8888, false) ?: return@withPermit TagOutcome(emptyList())
        try {
            runDetectors(key, argb)
        } finally {
            if (argb !== source) argb.recycle()
        }
    }

    private suspend fun runDetectors(key: String, argb: Bitmap): TagOutcome {
        val image = InputImage.fromBitmap(argb, 0)
        val run = DetectorRun(DETECTORS)
        val w = argb.width
        val h = argb.height
        run.step("face") { TagRules.faceTags(FaceScanner.assessChecked(key, argb)) }
        run.step("pose") {
            val points = pose.process(image).await().allPoseLandmarks.map { PoseLm(it.landmarkType, it.position.x / w, it.position.y / h, it.inFrameLikelihood) }
            TagRules.poseTags(points, bodyBox(PoseEvidence.confident(points).map { it.x * w to it.y * h }, w, h))
        }
        val personBoxes = run.hits.filter { it.tag == TagRules.FACE || it.tag == TagRules.BODY }.flatMap { it.boxes }
        val pass = if (DeviceLimits.analysisReduced()) emptyList() else zoomPass(argb, w, h)
        run.step("labels") { labelHits(image, pass, personBoxes) }
        run.step("objects") { objectHits(argb, image, w, h, personBoxes, pass) }
        run.step("text") {
            val result = text.process(image).await()
            val arabic = ArabicOcr.read(argb)
            TagRules.textTags(
                result.text + " " + arabic.joinToString(" ") { it.text },
                result.textBlocks.mapNotNull { block -> block.boundingBox?.let { box(it, w, h) } } + arabic.map { it.box }
            )
        }
        run.step("barcodes") {
            barcodes.process(image).await().flatMap { code -> TagRules.barcodeTags(code.valueType, listOfNotNull(code.boundingBox?.let { box(it, w, h) })) }
        }
        return run.outcome { SceneReasoner.refine(it).let(TagRules::merge).let(TagFamilies::withFamilies) }
    }

    private fun MutableMap<String, Float>.best(key: String, value: Float) {
        if (value > (this[key] ?: 0f)) this[key] = value
    }

    private class Spotted(val region: NormBox, val labels: List<Pair<String, Float>>, val fromTile: Boolean)

    private class TileResult(val rect: PxRect, val labels: Map<String, Float>, val objects: List<Spotted>)

    private suspend fun zoomPass(argb: Bitmap, w: Int, h: Int): List<TileResult> {
        val out = ArrayList<TileResult>()
        try {
            ZoomScan.forEachTile(argb) { tile ->
                val image = InputImage.fromBitmap(tile.bitmap, 0)
                val seen = HashMap<String, Float>()
                labeler.process(image).await().forEach { seen.best(it.text, it.confidence) }
                val found = ArrayList<Spotted>()
                objects.process(image).await().forEach { obj ->
                    val b = obj.boundingBox
                    val rect = tile.toFrame(b.left, b.top, b.right, b.bottom)
                    if (SmartZoom.cutByTile(rect, tile.rect, w, h)) return@forEach
                    val region = NormBox.fromPixels(rect.x.toFloat(), rect.y.toFloat(), rect.r.toFloat(), rect.b.toFloat(), w, h) ?: return@forEach
                    found += Spotted(region, obj.labels.map { it.text to it.confidence }, true)
                }
                out += TileResult(tile.rect, seen, found)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DetectorHealth.failed("zoom", e)
        }
        return out
    }

    private suspend fun labelHits(image: InputImage, pass: List<TileResult>, personBoxes: List<NormBox>): List<TagHit> {
        val whole = HashMap<String, Float>()
        labeler.process(image).await().forEach { whole.best(it.text, it.confidence) }
        val tiles = HashMap<String, MutableList<Float>>()
        pass.forEach { t -> t.labels.forEach { (text, score) -> tiles.getOrPut(text) { ArrayList() } += score } }
        return (whole.keys + tiles.keys).mapNotNull { text ->
            val score = SmartZoom.vote(whole[text], tiles[text].orEmpty()) ?: return@mapNotNull null
            val boxes = if (whole.containsKey(text)) emptyList() else localise(text, pass, personBoxes)
            TagRules.labelTag(text, score, boxes)
        }
    }

    private fun localise(text: String, pass: List<TileResult>, personBoxes: List<NormBox>): List<NormBox> =
        pass.filter { (it.labels[text] ?: 0f) >= SmartZoom.TILE_PAIR }
            .flatMap { it.objects }
            .map { it.region }
            .filter { !SceneReasoner.touchesPerson(it, personBoxes) }
            .sortedByDescending { it.width * it.height }
            .take(2)

    private suspend fun objectHits(argb: Bitmap, image: InputImage, w: Int, h: Int, personBoxes: List<NormBox>, pass: List<TileResult>): List<TagHit> {
        val spotted = ArrayList<Spotted>()
        objects.process(image).await().forEach { obj ->
            val region = box(obj.boundingBox, w, h) ?: return@forEach
            spotted += Spotted(region, obj.labels.map { it.text to it.confidence }, false)
        }
        spotted += newTileObjects(pass, w, h, spotted)
        val out = ArrayList<TagHit>()
        spotted.forEach { s ->
            val floor = if (s.fromTile) TILE_OBJECT_CONFIDENCE else OBJECT_CONFIDENCE
            s.labels.filter { it.second >= floor }.forEach { out += TagRules.objectTag(it.first, it.second, listOf(s.region)) }
        }
        spotted.sortedByDescending { it.region.width * it.region.height }.take(MAX_REGIONS).forEach { s ->
            val region = s.region.takeIf { it.width * w >= MIN_REGION && it.height * h >= MIN_REGION } ?: return@forEach
            if (SceneReasoner.touchesPerson(region, personBoxes)) return@forEach
            val rect = pixelRect(region.padded(REGION_PAD), w, h) ?: return@forEach
            val crop = ZoomScan.crop(argb, rect, ZOOM_REGION_TARGET, ZOOM_REGION_MAX) ?: return@forEach
            try {
                val floor = if (crop.scale > 1.5f) ZOOMED_LABEL_CONFIDENCE else 0f
                labeler.process(InputImage.fromBitmap(crop.bitmap, 0)).await().filter { it.confidence >= floor }.take(REGION_LABELS)
                    .forEach { out += TagRules.labelTag(it.text, it.confidence, listOf(region)) }
            } finally {
                crop.release()
            }
        }
        return out
    }

    private fun newTileObjects(pass: List<TileResult>, w: Int, h: Int, known: List<Spotted>): List<Spotted> {
        val knownPx = known.map { toPx(it.region, w, h) }
        return SmartZoom.suppress(pass.flatMap { it.objects }, { toPx(it.region, w, h) }).filter { extra ->
            val r = toPx(extra.region, w, h)
            knownPx.none { SmartZoom.iou(it, r) >= OBJECT_DUPLICATE_IOU || SmartZoom.containment(it, r) >= 0.7f }
        }
    }

    private fun toPx(box: NormBox, w: Int, h: Int): PxRect {
        val x = (box.l * w).toInt()
        val y = (box.t * h).toInt()
        return PxRect(x, y, ((box.r * w).toInt() - x).coerceAtLeast(1), ((box.b * h).toInt() - y).coerceAtLeast(1))
    }

    private fun pixelRect(box: NormBox, w: Int, h: Int): PxRect? = toPx(box, w, h).takeIf { it.w >= 8 && it.h >= 8 }

    private fun box(rect: android.graphics.Rect, w: Int, h: Int): NormBox? =
        NormBox.fromPixels(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(), w, h)

    private fun bodyBox(points: List<Pair<Float, Float>>, w: Int, h: Int): NormBox? {
        if (points.isEmpty()) return null
        val raw = NormBox.fromPixels(points.minOf { it.first }, points.minOf { it.second }, points.maxOf { it.first }, points.maxOf { it.second }, w, h)
        return raw?.padded(BODY_PAD)
    }
}
