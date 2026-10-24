package com.frameout.core.quality.content

import com.frameout.core.quality.TagText

enum class ContentGroup(val id: String, val en: String, val ar: String) {
    Vehicles("vehicles", "Vehicles", "مركبات"),
    Animals("animals", "Animals", "حيوانات"),
    Nature("nature", "Nature", "طبيعة"),
    Places("places", "Places", "أماكن ومبانٍ"),
    Food("food", "Food & drink", "طعام وشراب"),
    Activities("activities", "Sports & events", "رياضة وفعاليات"),
    Devices("devices", "Devices & objects", "أجهزة وأغراض"),
    Fashion("fashion", "Fashion", "أزياء"),
    People("people", "People & text", "أشخاص ونصوص");

    fun title(lang: String): String = when (lang) {
        "ar" -> ar
        else -> TagText.forLang(lang)?.contentGroups?.get(name) ?: en
    }
}

enum class ContentDetector { Label, Face, Smile, Group, Text, Code }

data class ContentTag(
    val id: String,
    val group: ContentGroup,
    val en: String,
    val ar: String,
    val labels: List<String> = emptyList(),
    val detector: ContentDetector = ContentDetector.Label
) {
    fun title(lang: String): String = when (lang) {
        "ar" -> ar
        else -> TagText.forLang(lang)?.contentTags?.get(id) ?: en
    }
}

internal fun tag(id: String, group: ContentGroup, en: String, ar: String, vararg labels: String) =
    ContentTag(id, group, en, ar, labels.toList())

internal fun special(id: String, en: String, ar: String, detector: ContentDetector) =
    ContentTag(id, ContentGroup.People, en, ar, emptyList(), detector)
