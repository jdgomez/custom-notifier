package dev.jdgomez.customnotifier.listing

import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Checks the store listing kept in `fastlane/metadata/android/` against Play's limits (spec: release-distribution). */
class StoreListingTest {
    // Unit tests run with the module directory as working directory.
    private val root = File("../fastlane/metadata/android")
    private val locales = listOf("en-US", "es-ES")

    private fun text(
        locale: String,
        name: String,
    ): String {
        val file = File(root, "$locale/$name.txt")
        assertTrue(file.isFile, "Missing $file")
        return file.readText().trim()
    }

    private fun size(file: File): Pair<Int, Int> {
        assertTrue(file.isFile, "Missing $file")
        val image = requireNotNull(ImageIO.read(file)) { "$file is not a readable image" }
        return image.width to image.height
    }

    private fun assertWithin(
        locale: String,
        name: String,
        limit: Int,
    ) {
        val value = text(locale, name)
        val length = value.codePointCount(0, value.length)
        assertTrue(length in 1..limit, "$locale/$name.txt has $length characters, allowed 1 to $limit")
    }

    @Test
    fun `every language has a title and descriptions within the limits`() {
        locales.forEach {
            assertWithin(it, "title", 30)
            assertWithin(it, "short_description", 80)
            assertWithin(it, "full_description", 4000)
        }
    }

    @Test
    fun `every language has at least two phone screenshots`() {
        locales.forEach { locale ->
            val screenshots = File(root, "$locale/images/phoneScreenshots").listFiles { f -> f.extension == "png" }.orEmpty()
            assertTrue(screenshots.size >= 2, "$locale has ${screenshots.size} phone screenshots, at least 2 needed")
            // Play: each side between 320 and 3840 px.
            screenshots.forEach {
                val (width, height) = size(it)
                assertTrue(minOf(width, height) >= 320 && maxOf(width, height) <= 3840, "$it is ${width}x$height")
            }
        }
    }

    @Test
    fun `icon and feature graphic have the exact sizes`() {
        assertEquals(512 to 512, size(File(root, "en-US/images/icon.png")))
        assertEquals(1024 to 500, size(File(root, "en-US/images/featureGraphic.png")))
    }
}
