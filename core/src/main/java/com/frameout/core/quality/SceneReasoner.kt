package com.frameout.core.quality

object SceneReasoner {
    const val LABEL_MIN = 0.70f
    const val OBJECT_MIN = 0.50f
    const val ANIMAL_WITH_PERSON_MIN = 0.92f
    const val NOISE_MIN = 0.88f
    const val PORTRAIT_FACE_AREA = 0.06f
    const val PORTRAIT_ANIMAL_MIN = 0.97f
    const val OVERLAP_ON_PERSON = 0.25f
    const val MAX_LABELS = 10
    private const val PERSON_LABEL = "l:Person"
    const val MIN_CONTEXT = 3
    const val FLICKER_MIN = 0.85f
    const val STABLE_SUPPORT = 0.6f
    const val STABLE_LABEL_MIN = 0.60f
    const val DUPLICATE_IOU = 0.6f
    const val VEHICLE_MIN = 0.70f
    const val FASHION_MIN = 0.60f
    const val COARSE_ON_VEHICLE_MIN = 0.90f
    const val STRONG_BODY = PoseEvidence.STRONG_CONFIDENT / PoseEvidence.LANDMARKS_TOTAL

    private val PERSON = setOf(
        "person", "people", "human", "human body", "girl", "boy", "man", "woman", "child", "toddler", "baby", "selfie",
        "smile", "hair", "face", "head", "skin", "forehead", "nose", "cheek", "chin", "eyebrow", "eye", "eyelash", "lip",
        "mouth", "neck", "shoulder", "hand", "finger", "arm", "leg", "hairstyle", "long hair", "beard", "facial hair",
        "jaw", "ear", "bangs", "black hair", "brown hair", "blond", "facial expression", "gesture", "thumb", "nail"
    )

    private val ANIMAL = setOf(
        "dog", "puppy", "cat", "kitten", "pet", "mammal", "carnivore", "canidae", "dog breed", "companion dog", "snout",
        "whiskers", "fur", "horse", "animal", "bird", "rabbit", "rodent", "cattle", "sheep", "pig", "livestock",
        "terrestrial animal", "working animal", "wildlife", "felidae", "small to medium-sized cats", "big cats", "fawn",
        "tail", "paw", "muzzle", "bear", "deer", "monkey", "primate", "vertebrate", "dog supply", "dog clothes",
        "sporting group", "toy dog", "hound", "terrier", "retriever", "fish", "reptile", "wolf", "fox"
    )

    private val NOISE = setOf(
        "fun", "leisure", "recreation", "event", "pattern", "design", "art", "rectangle", "circle", "material property",
        "electric blue", "magenta", "font", "happy", "style", "snapshot", "photography", "darkness", "composite material",
        "monochrome", "monochrome photography", "black and white", "tints and shades", "line", "symmetry"
    )

    private val GENDER = setOf("girl", "boy", "man", "woman")

    private val VEHICLE = setOf(
        "car", "vehicle", "sports car", "supercar", "race car", "performance car", "luxury vehicle", "motor vehicle", "land vehicle",
        "automotive design", "automotive exterior", "automotive lighting", "truck", "bus", "van", "motorcycle", "coupe", "sedan"
    )
    private val COARSE_PRONE_TO_VEHICLE = setOf("o:fashion_good", "o:home_good")

    private val PARENTS: Map<String, Set<String>> = mapOf(
        "dog" to setOf("mammal", "carnivore", "canidae", "pet", "animal", "vertebrate", "companion dog", "dog breed", "terrestrial animal"),
        "puppy" to setOf("dog", "mammal", "carnivore", "canidae", "pet", "animal", "vertebrate", "companion dog", "dog breed"),
        "cat" to setOf("mammal", "carnivore", "felidae", "pet", "animal", "vertebrate", "small to medium-sized cats"),
        "kitten" to setOf("cat", "mammal", "carnivore", "felidae", "pet", "animal", "vertebrate", "small to medium-sized cats"),
        "horse" to setOf("mammal", "animal", "vertebrate", "livestock", "working animal"),
        "bird" to setOf("animal", "vertebrate"),
        "girl" to setOf("person", "human", "people", "child"),
        "boy" to setOf("person", "human", "people", "child"),
        "man" to setOf("person", "human", "people"),
        "woman" to setOf("person", "human", "people"),
        "baby" to setOf("person", "human", "people", "child", "toddler"),
        "toddler" to setOf("person", "human", "people", "child"),
        "selfie" to setOf("person", "human", "people"),
        "smile" to setOf("facial expression", "gesture"),
        "long hair" to setOf("hair", "hairstyle"),
        "beard" to setOf("facial hair"),
        "sports car" to setOf("car", "vehicle", "land vehicle", "motor vehicle", "automotive design"),
        "car" to setOf("vehicle", "land vehicle", "motor vehicle"),
        "truck" to setOf("vehicle", "land vehicle", "motor vehicle"),
        "motorcycle" to setOf("vehicle", "land vehicle", "motor vehicle"),
        "bus" to setOf("vehicle", "land vehicle", "motor vehicle"),
        "bicycle" to setOf("vehicle", "bicycle wheel", "wheel"),
        "airplane" to setOf("aircraft", "vehicle"),
        "tree" to setOf("plant", "woody plant"),
        "flower" to setOf("plant", "petal"),
        "pizza" to setOf("food", "dish", "cuisine", "fast food", "baked goods"),
        "cake" to setOf("food", "dish", "baked goods", "dessert"),
        "sunset" to setOf("sky", "dusk", "afterglow"),
        "beach" to setOf("coast", "shore", "sand"),
        "skyscraper" to setOf("building", "tower block", "urban area"),
        "mountain" to setOf("hill", "highland")
    )

    fun refine(hits: List<TagHit>, neighbors: List<List<TagHit>> = emptyList()): List<TagHit> {
        if (hits.isEmpty()) return hits
        val context = if (neighbors.size >= MIN_CONTEXT) neighbors.map { refine(it) } else emptyList()
        fun support(tag: String): Float = if (context.isEmpty()) -1f else context.count { n -> n.any { it.tag == tag } }.toFloat() / context.size
        val personContext = context.isNotEmpty() &&
            context.count { n -> n.any { it.tag == TagRules.FACE || it.tag == TagRules.BODY } }.toFloat() / context.size >= 0.5f

        val faces = hits.firstOrNull { it.tag == TagRules.FACE }?.boxes.orEmpty()
        val vehicle = vehicleDominant(hits)
        val bodyConfirmed = bodyConfirmed(hits, faces.isNotEmpty(), vehicle)
        val bodies = if (bodyConfirmed) hits.firstOrNull { it.tag == TagRules.BODY }?.boxes.orEmpty() else emptyList()
        val personBoxes = faces + bodies
        val personPresent = faces.isNotEmpty() || bodies.isNotEmpty() || personContext
        val portrait = faces.any { it.width * it.height >= PORTRAIT_FACE_AREA }
        val hasText = hits.any { it.tag == TagRules.TEXT }

        val kept = ArrayList<TagHit>(hits.size)
        for (hit in hits) {
            val sup = support(hit.tag)
            when {
                hit.tag.startsWith(TagRules.LABEL) -> {
                    val min = if (sup >= STABLE_SUPPORT) STABLE_LABEL_MIN else LABEL_MIN
                    if (keepLabel(hit, min, personPresent, personBoxes, portrait, hasText) && !flicker(hit, sup)) kept += stabilised(hit, sup)
                }
                hit.tag.startsWith(TagRules.OBJECT) -> if (keepObject(hit, personBoxes, vehicle) && !(sup == 0f && hit.confidence < 0.7f)) kept += hit
                hit.tag == TagRules.BODY -> if (bodyConfirmed) kept += hit
                else -> kept += hit
            }
        }
        var labels = kept.filter { it.tag.startsWith(TagRules.LABEL) }.toMutableList()
        val others = kept.filterNot { it.tag.startsWith(TagRules.LABEL) }

        labels = withPersonLabel(labels, hits, faces, bodies)

        labels = exclusive(labels, faces.size).toMutableList()
        labels = hideParents(labels).toMutableList()
        labels.sortByDescending { it.confidence }
        return (others + labels.take(MAX_LABELS)).map { it.copy(boxes = dedupe(it.boxes)) }
    }

    private fun withPersonLabel(labels: MutableList<TagHit>, hits: List<TagHit>, faces: List<NormBox>, bodies: List<NormBox>): MutableList<TagHit> {
        if (faces.isEmpty() && bodies.isEmpty()) return labels
        if (labels.any { name(it) in PERSON_CORE }) return labels
        val evidence = if (bodies.isNotEmpty()) bodies else faces
        val confidence = hits.filter { it.tag == TagRules.FACE || it.tag == TagRules.BODY }.maxOfOrNull { it.confidence }?.coerceAtLeast(0.9f) ?: 0.9f
        labels += TagHit(PERSON_LABEL, confidence, evidence)
        return labels
    }

    private val PERSON_CORE = setOf("person", "people", "human", "girl", "boy", "man", "woman", "child", "baby", "toddler")

    private fun flicker(hit: TagHit, support: Float) = support == 0f && hit.confidence < FLICKER_MIN

    private fun stabilised(hit: TagHit, support: Float) =
        if (support >= STABLE_SUPPORT) hit.copy(confidence = (hit.confidence + 0.05f).coerceAtMost(1f)) else hit

    internal fun dedupe(boxes: List<NormBox>): List<NormBox> {
        if (boxes.size < 2) return boxes
        val out = ArrayList<NormBox>()
        for (b in boxes.sortedByDescending { it.width * it.height }) {
            if (out.none { iou(it, b) >= DUPLICATE_IOU }) out += b
        }
        return out
    }

    internal fun iou(a: NormBox, b: NormBox): Float {
        val w = minOf(a.r, b.r) - maxOf(a.l, b.l)
        val h = minOf(a.b, b.b) - maxOf(a.t, b.t)
        if (w <= 0f || h <= 0f) return 0f
        val inter = w * h
        return inter / (a.width * a.height + b.width * b.height - inter)
    }

    private fun name(hit: TagHit) = hit.tag.substringAfter(':').trim().lowercase()

    private fun keepLabel(hit: TagHit, min: Float, personPresent: Boolean, personBoxes: List<NormBox>, portrait: Boolean, hasText: Boolean): Boolean {
        val n = name(hit)
        if (hit.confidence < min) return false
        if (n in NOISE) return hit.confidence >= NOISE_MIN && !(hasText && (n == "font"))
        if (n in ANIMAL) {
            if (!personPresent) return true
            if (hit.boxes.any { touchesPerson(it, personBoxes) }) return false
            if (portrait) return hit.confidence >= PORTRAIT_ANIMAL_MIN
            return hit.confidence >= ANIMAL_WITH_PERSON_MIN
        }
        return true
    }

    private fun keepObject(hit: TagHit, personBoxes: List<NormBox>, vehicle: Boolean): Boolean {
        if (hit.confidence < OBJECT_MIN) return false
        if (vehicle && hit.tag in COARSE_PRONE_TO_VEHICLE && hit.confidence < COARSE_ON_VEHICLE_MIN) return false
        if (hit.tag == "o:fashion_good") return personBoxes.isNotEmpty() && hit.confidence >= FASHION_MIN
        if (personBoxes.isEmpty()) return true
        return hit.boxes.none { overlap(it, personBoxes) >= 0.5f }
    }

    internal fun vehicleDominant(hits: List<TagHit>): Boolean =
        hits.any { it.tag.startsWith(TagRules.LABEL) && name(it) in VEHICLE && it.confidence >= VEHICLE_MIN }

    internal fun hasPersonEvidence(hits: List<TagHit>): Boolean = hits.any {
        it.tag == TagRules.FACE || it.tag == TagRules.BODY ||
            (it.tag.startsWith(TagRules.LABEL) && name(it) in PERSON_CORE && it.confidence >= LABEL_MIN)
    }

    private fun bodyConfirmed(hits: List<TagHit>, hasFace: Boolean, vehicle: Boolean): Boolean {
        val body = hits.firstOrNull { it.tag == TagRules.BODY } ?: return false
        if (hasFace) return true
        val personLabel = hits.any { it.tag.startsWith(TagRules.LABEL) && name(it) in PERSON_CORE && it.confidence >= LABEL_MIN }
        return personLabel || (!vehicle && body.confidence >= STRONG_BODY)
    }

    private fun exclusive(labels: List<TagHit>, faceCount: Int): List<TagHit> {
        var out = labels
        val genders = out.filter { name(it) in GENDER }
        if (genders.size > 1 && faceCount < 2) {
            val best = genders.maxBy { it.confidence }
            out = out.filter { name(it) !in GENDER || it === best }
        }
        val dog = out.firstOrNull { name(it) == "dog" }
        val cat = out.firstOrNull { name(it) == "cat" }
        if (dog != null && cat != null && !(dog.confidence >= 0.85f && cat.confidence >= 0.85f)) {
            val loser = if (dog.confidence >= cat.confidence) cat else dog
            out = out.filter { it !== loser }
        }
        return out
    }

    private fun hideParents(labels: List<TagHit>): List<TagHit> {
        val present = labels.map { name(it) }.toSet()
        val hidden = HashSet<String>()
        present.forEach { child -> PARENTS[child]?.let { hidden += it } }
        return labels.filter { name(it) !in hidden }
    }

    internal fun touchesPerson(box: NormBox, personBoxes: List<NormBox>): Boolean =
        overlap(box, personBoxes) >= OVERLAP_ON_PERSON ||
            personBoxes.any { p -> (p.l + p.r) / 2f in box.l..box.r && (p.t + p.b) / 2f in box.t..box.b }

    internal fun overlap(box: NormBox, others: List<NormBox>): Float {
        val area = box.width * box.height
        if (area <= 0f) return 0f
        var best = 0f
        for (o in others) {
            val w = minOf(box.r, o.r) - maxOf(box.l, o.l)
            val h = minOf(box.b, o.b) - maxOf(box.t, o.t)
            if (w > 0f && h > 0f) best = maxOf(best, w * h / area)
        }
        return best
    }
}
