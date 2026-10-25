package com.frameout.core

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringResourcesTest {
    private val locales = listOf("values-ar", "values-tr", "values-ru", "values-fa", "values-ur", "values-fr", "values-pt", "values-hi", "values-ko", "values-ja", "values-zh", "values-de", "values-ro", "values-es", "values-uk", "values-it", "values-nb", "values-ms")

    private fun parse(dir: String, file: String) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$dir/$file"))

    private fun load(dir: String): Map<String, String> {
        val nodes = parse(dir, "strings.xml").getElementsByTagName("string")
        return (0 until nodes.length).associate { nodes.item(it).attributes.getNamedItem("name").nodeValue to nodes.item(it).textContent }
    }

    private fun plurals(dir: String): Map<String, List<String>> {
        val nodes = parse(dir, "plurals.xml").getElementsByTagName("plurals")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            val items = node.childNodes
            node.attributes.getNamedItem("name").nodeValue to (0 until items.length)
                .map { items.item(it) }
                .filter { it.nodeName == "item" }
                .map { it.attributes.getNamedItem("quantity").nodeValue }
        }
    }

    private fun placeholders(value: String): List<String> =
        Regex("%\\d\\$[sd]").findAll(value).map { it.value }.sorted().toList()

    @Test
    fun everyLocaleHasSameKeys() {
        val base = load("values")
        assertTrue(base.isNotEmpty())
        locales.forEach { assertEquals(it, base.keys, load(it).keys) }
    }

    @Test
    fun placeholdersMatchAcrossLocales() {
        val base = load("values")
        locales.forEach { dir ->
            val other = load(dir)
            base.forEach { (key, value) -> assertEquals("$dir/$key", placeholders(value), placeholders(other.getValue(key))) }
        }
    }

    @Test
    fun everyLocaleHasSamePlurals() {
        val base = plurals("values")
        assertTrue(base.isNotEmpty())
        locales.forEach { assertEquals(it, base.keys, plurals(it).keys) }
    }

    @Test
    fun russianPluralsCoverAllCategories() {
        plurals("values-ru").forEach { (name, quantities) ->
            assertEquals(name, listOf("one", "few", "many", "other"), quantities)
        }
    }

    @Test
    fun ukrainianPluralsCoverAllCategories() {
        plurals("values-uk").forEach { (name, quantities) ->
            assertEquals(name, listOf("one", "few", "many", "other"), quantities)
        }
    }

    @Test
    fun persianPluralsHaveOneAndOther() {
        plurals("values-fa").forEach { (name, quantities) -> assertEquals(name, listOf("one", "other"), quantities) }
    }

    @Test
    fun urduPluralsHaveOneAndOther() {
        plurals("values-ur").forEach { (name, quantities) -> assertEquals(name, listOf("one", "other"), quantities) }
    }

    @Test
    fun frenchPluralsCoverOneManyOther() {
        plurals("values-fr").forEach { (name, quantities) -> assertEquals(name, listOf("one", "many", "other"), quantities) }
    }

    @Test
    fun portuguesePluralsCoverOneManyOther() {
        plurals("values-pt").forEach { (name, quantities) -> assertEquals(name, listOf("one", "many", "other"), quantities) }
    }

    @Test
    fun hindiPluralsHaveOneAndOther() {
        plurals("values-hi").forEach { (name, quantities) -> assertEquals(name, listOf("one", "other"), quantities) }
    }

    @Test
    fun turkishPluralsHaveOneAndOther() {
        plurals("values-tr").forEach { (name, quantities) -> assertEquals(name, listOf("one", "other"), quantities) }
    }

    @Test
    fun germanPluralsHaveOneAndOther() {
        plurals("values-de").forEach { (name, quantities) -> assertEquals(name, listOf("one", "other"), quantities) }
    }

    @Test
    fun romanianPluralsCoverOneFewOther() {
        plurals("values-ro").forEach { (name, quantities) -> assertEquals(name, listOf("one", "few", "other"), quantities) }
    }

    @Test
    fun spanishPluralsCoverOneManyOther() {
        plurals("values-es").forEach { (name, quantities) -> assertEquals(name, listOf("one", "many", "other"), quantities) }
    }

    @Test
    fun italianPluralsCoverOneManyOther() {
        plurals("values-it").forEach { (name, quantities) -> assertEquals(name, listOf("one", "many", "other"), quantities) }
    }

    @Test
    fun norwegianPluralsHaveOneAndOther() {
        plurals("values-nb").forEach { (name, quantities) -> assertEquals(name, listOf("one", "other"), quantities) }
    }

    @Test
    fun malayPluralsHaveOnlyOther() {
        plurals("values-ms").forEach { (name, quantities) -> assertEquals(name, listOf("other"), quantities) }
    }

    @Test
    fun koreanPluralsHaveOnlyOther() {
        plurals("values-ko").forEach { (name, quantities) -> assertEquals(name, listOf("other"), quantities) }
    }

    @Test
    fun japanesePluralsHaveOnlyOther() {
        plurals("values-ja").forEach { (name, quantities) -> assertEquals(name, listOf("other"), quantities) }
    }

    @Test
    fun chinesePluralsHaveOnlyOther() {
        plurals("values-zh").forEach { (name, quantities) -> assertEquals(name, listOf("other"), quantities) }
    }
}
