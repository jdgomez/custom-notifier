package dev.jdgomez.customnotifier.versioning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AppVersionTest {
    @Test
    fun `version code grows with every later version`() {
        val ordered = listOf("0.1.0", "0.2.0", "0.2.1", "0.9.3", "0.10.0", "0.999.999", "1.0.0", "1.0.1", "2.0.0")
        val codes = ordered.map { AppVersion.parse(it).code }
        assertEquals(codes.sorted(), codes)
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `version code is derived from the parts`() {
        val version = AppVersion.parse("1.2.3\n")
        assertEquals("1.2.3", version.name)
        assertEquals(1_002_003, version.code)
    }

    @Test
    fun `highest supported version stays within the Play limit`() {
        assertTrue(AppVersion.parse("2099.999.999").code <= 2_100_000_000)
    }

    @Test
    fun `rejects anything but a plain semantic version`() {
        listOf("", "1", "1.2", "1.2.3.4", "v1.2.3", "1.2.3-rc1", "1.2.x", "01.2.3", "-1.2.3").forEach {
            assertFailsWith<IllegalArgumentException>(it) { AppVersion.parse(it) }
        }
    }

    @Test
    fun `rejects parts that would stop the code from growing`() {
        listOf("0.1000.0", "0.0.1000", "2100.0.0", "99999999999.0.0").forEach {
            assertFailsWith<IllegalArgumentException>(it) { AppVersion.parse(it) }
        }
    }
}
