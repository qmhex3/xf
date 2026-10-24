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
    private val translated = listOf("tr", "ru", "fa", "ur", "fr", "hi", "ko", "ja", "zh")

    @Test
    fun appLangMapsSupportedAndFallsBack() {
        assertEquals("tr", AppLang.of("tr-TR"))
        assertEquals("ru", AppLang.of("RU"))
        assertEquals("ar", AppLang.of("ar_EG"))
        assertEquals("fa", AppLang.of("fa-IR"))
        assertEquals("ur", AppLang.of("ur-PK"))
        assertEquals("fr", AppLang.of("fr-CA"))
        assertEquals("hi", AppLang.of("hi-IN"))
        assertEquals("ko", AppLang.of("ko-KR"))
        assertEquals("ja", AppLang.of("ja-JP"))
        assertEquals("zh", AppLang.of("zh-CN"))
        assertEquals("zh", AppLang.of("zh_TW"))
        assertEquals("en", AppLang.of("de"))
        assertEquals("en", AppLang.of(null))
    }

    @Test
    fun labelsAreTranslated() {
        assertEquals("Araba", TagNames.display("l:Car", "tr"))
        assertEquals("Автомобиль", TagNames.display("l:Car", "ru"))
        assertEquals("Voiture", TagNames.display("l:Car", "fr"))
        assertEquals("कार", TagNames.display("l:Car", "hi"))
        assertEquals("자동차", TagNames.display("l:Car", "ko"))
        assertEquals("自動車", TagNames.display("l:Car", "ja"))
        assertEquals("汽车", TagNames.display("l:Car", "zh"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "ko"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "zh"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "hi"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "fr"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "ru"))
        assertEquals("Unknown thing", TagNames.display("l:Unknown thing", "tr"))
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
