package dev.jdgomez.customnotifier.localization

import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringsParityTest {
    private val placeholder = Regex("%(\\d+\\$)?[sdf]")

    /** The translatable string entries of a resource file, by name, with their placeholders. */
    private fun placeholders(path: String): Map<String, Set<String>> {
        val strings =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(File(path))
                .getElementsByTagName("string")
        return (0 until strings.length)
            .map { strings.item(it) }
            .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
            .associate { it.attributes.getNamedItem("name").nodeValue to placeholder.findAll(it.textContent).map { m -> m.value }.toSet() }
    }

    private val english = placeholders("src/main/res/values/strings.xml")
    private val spanish = placeholders("src/main/res/values-es/strings.xml")

    @Test
    fun `Spanish has exactly the translatable keys of English`() {
        assertTrue(english.isNotEmpty())
        assertEquals(english.keys, spanish.keys)
    }

    @Test
    fun `every Spanish text has the placeholders of its English text`() {
        english.forEach { (key, expected) -> assertEquals(expected, spanish[key], "placeholders of $key") }
    }
}
