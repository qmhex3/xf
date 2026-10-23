package com.frameout.core.quality

import com.frameout.core.db.CoverLane
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.FrameTagEntity

object CoverLanes {
    const val MAX_LANES = 4
    const val PER_LANE = 12
    const val POOL = 90

    private val generic = setOf(
        "vehicle", "land vehicle", "motor vehicle", "animal", "mammal", "pet", "person", "people", "product", "vertebrate",
        "carnivore", "terrestrial animal", "wildlife", "livestock", "working animal", "fun", "leisure", "event", "sports",
        "technology", "fashion", "clothing", "food", "dish", "landscape", "interaction", "standing", "sitting", "monochrome", "pattern"
    )

    private val alias: Map<String, String> = HashMap<String, String>().also { out ->
        fun group(name: String, vararg members: String) = members.forEach { out[it.lowercase()] = name }
        group("Airplane", "Airplane", "Airliner", "Aircraft", "Aviation", "Aerospace engineering", "Wing")
        group("Car", "Car", "Bumper", "Windshield", "Wheel", "Tire", "Odometer", "Sports car", "Automotive design")
        group("Bicycle", "Bicycle", "Unicycle", "Bicycle wheel")
        group("Boat", "Boat", "Sailboat", "Speedboat", "Skiff", "Canoe", "Kayak", "Brig", "Frigate", "Clipper", "Ship")
        group("Van", "Van", "Truck", "Tractor")
        group("Dog", "Dog", "Shetland sheepdog", "Dalmatian", "Basset hound", "Cairn terrier", "Cavalier", "Boxer", "Shikoku", "Puppy", "Hound", "Terrier", "Retriever", "Dog breed", "Companion dog")
        group("Cat", "Cat", "Sphynx", "Ragdoll", "Pixie-bob", "Kitten", "Felidae", "Small to medium-sized cats", "Big cats")
        group("Bird", "Bird", "Waterfowl", "Duck", "Penguin")
        group("Food", "Food", "Cuisine", "Meal", "Lunch", "Supper", "Bento", "Couscous", "Dish")
        group("Swimming", "Swimming", "Pool", "Scuba diving", "Snorkeling", "Underwater")
    }

    fun build(tag: String, frames: List<FrameEntity>, labels: List<FrameTagEntity>): List<CoverLane> {
        if (frames.isEmpty()) return emptyList()
        val family = if (TagFamilies.isFamily(tag)) tag.removePrefix(TagFamilies.PREFIX) else null
        if (family != null) {
            val byFrame = labels.groupBy { it.frameId }
            val groups = LinkedHashMap<String, MutableList<FrameEntity>>()
            frames.forEach { frame ->
                kindOf(family, byFrame[frame.id].orEmpty())?.let { groups.getOrPut(it) { ArrayList() } += frame }
            }
            val ranked = groups.entries.sortedWith(compareByDescending<Map.Entry<String, MutableList<FrameEntity>>> { it.value.size }.thenBy { it.key })
            if (ranked.size >= 2) return ranked.take(MAX_LANES).map { CoverLane(it.key, spread(it.value, PER_LANE)) }
        }
        return chunks(frames)
    }

    fun sequence(lanes: List<CoverLane>): List<Pair<Int, FrameEntity>> {
        val out = ArrayList<Pair<Int, FrameEntity>>()
        val rounds = lanes.maxOfOrNull { it.frames.size } ?: 0
        for (round in 0 until rounds) lanes.forEachIndexed { i, lane -> lane.frames.getOrNull(round)?.let { out += i to it } }
        return out
    }

    internal fun kindOf(family: String, labels: List<FrameTagEntity>): String? {
        val mine = labels
            .map { it.tag.removePrefix(TagRules.LABEL) to it.confidence }
            .filter { TagFamilies.familyOfLabel(it.first) == family }
            .sortedByDescending { it.second }
            .map { alias[it.first.trim().lowercase()] ?: it.first.trim() }
        return mine.firstOrNull { it.lowercase() !in generic } ?: mine.firstOrNull()
    }

    internal fun spread(frames: List<FrameEntity>, limit: Int): List<FrameEntity> {
        val good = frames.take(maxOf(limit, frames.size * 2 / 3)).sortedBy { it.index }
        if (good.size <= limit) return good
        val step = good.size.toFloat() / limit
        return (0 until limit).map { good[(it * step).toInt()] }
    }

    private fun chunks(frames: List<FrameEntity>): List<CoverLane> {
        val lanes = minOf(MAX_LANES, frames.size)
        val ordered = spread(frames, lanes * PER_LANE)
        return (0 until lanes).map { i -> CoverLane(null, ordered.filterIndexed { idx, _ -> idx % lanes == i }) }
    }
}
