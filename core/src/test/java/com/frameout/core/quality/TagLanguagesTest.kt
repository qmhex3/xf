package com.frameout.core.quality

import com.frameout.core.quality.content.ContentCatalog
import com.frameout.core.quality.content.ContentGroup
import com.frameout.core.search.ImageSearchEngines
import com.frameout.core.util.AppLang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagLanguagesTest {
    private val translated = listOf("tr", "ru", "fa", "ur", "fr", "pt", "hi", "ko", "ja", "zh", "zh-TW", "de", "ro", "es", "uk", "it", "nb", "ms", "id", "bn", "th", "vi", "nl", "pl", "sv", "fil")

    @Test
    fun appLangMapsSupportedAndFallsBack() {
        assertEquals("tr", AppLang.of("tr-TR"))
        assertEquals("ru", AppLang.of("RU"))
        assertEquals("ar", AppLang.of("ar_EG"))
        assertEquals("fa", AppLang.of("fa-IR"))
        assertEquals("ur", AppLang.of("ur-PK"))
        assertEquals("fr", AppLang.of("fr-CA"))
        assertEquals("pt", AppLang.of("pt-BR"))
        assertEquals("pt", AppLang.of("pt_PT"))
        assertEquals("hi", AppLang.of("hi-IN"))
        assertEquals("bn", AppLang.of("bn-BD"))
        assertEquals("bn", AppLang.of("bn_IN"))
        assertEquals("th", AppLang.of("th-TH"))
        assertEquals("th", AppLang.of("th"))
        assertEquals("vi", AppLang.of("vi-VN"))
        assertEquals("nl", AppLang.of("nl-NL"))
        assertEquals("nl", AppLang.of("nl_BE"))
        assertEquals("nl", AppLang.of("NL"))
        assertEquals("pl", AppLang.of("pl-PL"))
        assertEquals("pl", AppLang.of("pl_PL"))
        assertEquals("pl", AppLang.of("PL"))
        assertEquals("sv", AppLang.of("sv-SE"))
        assertEquals("sv", AppLang.of("sv_FI"))
        assertEquals("sv", AppLang.of("SV"))
        assertEquals("fil", AppLang.of("fil-PH"))
        assertEquals("fil", AppLang.of("fil_PH"))
        assertEquals("fil", AppLang.of("tl-PH"))
        assertEquals("fil", AppLang.of("tl"))
        assertEquals("fil", AppLang.of("FIL"))
        assertEquals("vi", AppLang.of("vi"))
        assertEquals("vi", AppLang.of("VI"))
        assertEquals("vi", AppLang.of("vi_VN"))
        assertEquals("ko", AppLang.of("ko-KR"))
        assertEquals("ja", AppLang.of("ja-JP"))
        assertEquals("zh", AppLang.of("zh-CN"))
        assertEquals("zh-TW", AppLang.of("zh_TW"))
        assertEquals("zh-TW", AppLang.of("zh-Hant"))
        assertEquals("zh-TW", AppLang.of("zh-Hant-TW"))
        assertEquals("zh-TW", AppLang.of("zh-HK"))
        assertEquals("zh", AppLang.of("zh-Hans-CN"))
        assertEquals("zh", AppLang.of("zh-Hans-TW"))
        assertEquals("zh", AppLang.of("zh"))
        assertEquals("de", AppLang.of("de-DE"))
        assertEquals("de", AppLang.of("de_AT"))
        assertEquals("ro", AppLang.of("ro-RO"))
        assertEquals("ro", AppLang.of("ro_MD"))
        assertEquals("es", AppLang.of("es-ES"))
        assertEquals("es", AppLang.of("es_MX"))
        assertEquals("uk", AppLang.of("uk-UA"))
        assertEquals("uk", AppLang.of("UK"))
        assertEquals("it", AppLang.of("it-IT"))
        assertEquals("it", AppLang.of("IT"))
        assertEquals("it", AppLang.of("it_CH"))
        assertEquals("nb", AppLang.of("nb-NO"))
        assertEquals("nb", AppLang.of("NB"))
        assertEquals("nb", AppLang.of("no"))
        assertEquals("nb", AppLang.of("nn-NO"))
        assertEquals("ms", AppLang.of("ms-MY"))
        assertEquals("ms", AppLang.of("MS"))
        assertEquals("ms", AppLang.of("ms_BN"))
        assertEquals("id", AppLang.of("id-ID"))
        assertEquals("id", AppLang.of("ID"))
        assertEquals("id", AppLang.of("in"))
        assertEquals("id", AppLang.of("in_ID"))
        assertEquals("en", AppLang.of("fi"))
        assertEquals("en", AppLang.of(null))
    }

    @Test
    fun labelsAreTranslated() {
        assertEquals("Araba", TagNames.display("l:Car", "tr"))
        assertEquals("Автомобиль", TagNames.display("l:Car", "ru"))
        assertEquals("Voiture", TagNames.display("l:Car", "fr"))
        assertEquals("Carro", TagNames.display("l:Car", "pt"))
        assertEquals("कार", TagNames.display("l:Car", "hi"))
        assertEquals("গাড়ি", TagNames.display("l:Car", "bn"))
        assertEquals("자동차", TagNames.display("l:Car", "ko"))
        assertEquals("自動車", TagNames.display("l:Car", "ja"))
        assertEquals("汽车", TagNames.display("l:Car", "zh"))
        assertEquals("Auto", TagNames.display("l:Car", "de"))
        assertEquals("Mașină", TagNames.display("l:Car", "ro"))
        assertEquals("Automóvil", TagNames.display("l:Car", "es"))
        assertEquals("Автомобіль", TagNames.display("l:Car", "uk"))
        assertEquals("Automobile", TagNames.display("l:Car", "it"))
        assertEquals("Bil", TagNames.display("l:Car", "nb"))
        assertEquals("Kereta", TagNames.display("l:Car", "ms"))
        assertEquals("Mobil", TagNames.display("l:Car", "id"))
        assertEquals("รถยนต์", TagNames.display("l:Car", "th"))
        assertEquals("Ô tô", TagNames.display("l:Car", "vi"))
        assertEquals("Auto", TagNames.display("l:Car", "nl"))
        assertEquals("Samochód", TagNames.display("l:Car", "pl"))
        assertEquals("Bil", TagNames.display("l:Car", "sv"))
        assertEquals("Kotse", TagNames.display("l:Car", "fil"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "ko"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "zh"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "hi"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "fr"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "pt"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "ru"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "tr"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "de"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "ro"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "es"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "uk"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "it"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "nb"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "ms"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "id"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "th"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "vi"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "nl"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "pl"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "sv"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "fil"))
    }

    @Test
    fun everyTagFamilyAndGroupHasTitles() {
        translated.forEach { lang ->
            TagFamilies.all.forEach { assertNotEquals("${it.id}/$lang", "", it.title(lang)) }
            TagGroup.values().forEach { assertNotEquals("", TagNames.groupTitle(it, lang)) }
            ContentGroup.values().forEach { assertNotEquals("", it.title(lang)) }
        }
    }

    @Test
    fun everyContentTagHasTranslationInEveryLanguage() {
        translated.forEach { lang ->
            val text = TagText.forLang(lang)!!
            ContentCatalog.all.forEach { assertTrue("${it.id}/$lang", text.contentTags.containsKey(it.id)) }
        }
    }

    @Test
    fun everySearchEngineHasDescriptions() {
        translated.forEach { lang ->
            ImageSearchEngines.all.forEach { assertTrue("${it.id}/$lang", TagText.forLang(lang)!!.engines.containsKey(it.id)) }
        }
    }

    @Test
    fun fixedTagsAreTranslated() {
        assertEquals("Лица", TagNames.display(TagRules.FACE, "ru"))
        translated.forEach { assertNotEquals(TagNames.display(TagRules.FACE, "en"), "") }
    }
}
