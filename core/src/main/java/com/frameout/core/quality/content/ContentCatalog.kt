package com.frameout.core.quality.content

object ContentCatalog {
    val all: List<ContentTag> = ContentTagsMoving.vehicles + ContentTagsMoving.animals + ContentTagsMoving.nature +
        ContentTagsLife.places + ContentTagsLife.food + ContentTagsLife.activities +
        ContentTagsThings.devices + ContentTagsThings.fashion + ContentTagsThings.people

    private val byId: Map<String, ContentTag> = all.associateBy { it.id }

    fun find(id: String): ContentTag? = byId[id]

    fun inGroup(group: ContentGroup): List<ContentTag> = all.filter { it.group == group }

    fun known(ids: Collection<String>): Set<String> = ids.filterTo(LinkedHashSet()) { it in byId }

    fun selected(ids: Collection<String>): List<ContentTag> = ids.mapNotNull { byId[it] }

    fun summary(ids: Collection<String>, lang: String, limit: Int = 3): String {
        val names = selected(ids).map { it.title(lang) }
        val separator = when (lang) {
            "ar", "fa", "ur" -> "، "
            "zh", "zh-TW" -> "、"
            else -> ", "
        }
        if (names.size <= limit) return names.joinToString(separator)
        return names.take(limit).joinToString(separator) + " +" + (names.size - limit)
    }
}
