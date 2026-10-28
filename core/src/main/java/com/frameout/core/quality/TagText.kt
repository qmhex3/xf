package com.frameout.core.quality

import com.frameout.core.util.AppLang

class TagText(
    private val labels: Map<String, String>,
    val fixed: Map<String, String>,
    val groups: Map<String, String>,
    val families: Map<String, String>,
    val contentTags: Map<String, String>,
    val contentGroups: Map<String, String>,
    val engines: Map<String, String>,
    val compare: Map<String, String>
) {
    private val lowerLabels: Map<String, String> by lazy {
        HashMap<String, String>().also { out -> labels.forEach { (k, v) -> out[k.lowercase()] = v } }
    }

    fun label(name: String): String? = labels[name] ?: lowerLabels[name.lowercase()]

    companion object {
        fun forLang(lang: String): TagText? = when (AppLang.of(lang)) {
            "tr" -> TagTextTr.text
            "ru" -> TagTextRu.text
            "fa" -> TagTextFa.text
            "ur" -> TagTextUr.text
            "fr" -> TagTextFr.text
            "pt" -> TagTextPt.text
            "ko" -> TagTextKo.text
            "ja" -> TagTextJa.text
            "zh" -> TagTextZh.text
            "zh-TW" -> TagTextZhTw.text
            "hi" -> TagTextHi.text
            "de" -> TagTextDe.text
            "ro" -> TagTextRo.text
            "es" -> TagTextEs.text
            "uk" -> TagTextUk.text
            "it" -> TagTextIt.text
            "nb" -> TagTextNb.text
            "ms" -> TagTextMs.text
            "id" -> TagTextId.text
            "bn" -> TagTextBn.text
            "th" -> TagTextTh.text
            "vi" -> TagTextVi.text
            "nl" -> TagTextNl.text
            "pl" -> TagTextPl.text
            "sv" -> TagTextSv.text
            "fil" -> TagTextFil.text
            else -> null
        }
    }
}
