package dev.jdgomez.customnotifier.domain

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProductTest {
    private val t0 = Instant.parse("2026-01-01T00:00:00Z")

    private fun days(n: Long) = t0 + Duration.ofDays(n)

    private fun product(
        initialUnits: Int = 30,
        packageSize: Int = 90,
        rate: ConsumptionRate = ConsumptionRate(1, 1),
    ) = Product.create(ProductName("Vitamin D"), UnitLabel("pill"), PackageSize(packageSize), rate, initialUnits, t0)

    private fun Quantity.half() = this / 2

    @Test
    fun `valid product starts at its initial stock`() {
        val product =
            Product.create(ProductName(" Vitamin D "), UnitLabel("pill"), PackageSize(90), ConsumptionRate(1, 1), 30, t0)
        assertEquals("Vitamin D", product.name.value)
        assertEquals("pill", product.unitLabel.value)
        assertEquals(Quantity.of(30), product.estimatedStockAt(t0).exact)
    }

    @Test
    fun `negative initial stock is rejected naming the stock`() {
        val error = assertFailsWith<IllegalArgumentException> { product(initialUnits = -1) }
        assertTrue(error.message!!.contains("stock"))
    }

    @Test
    fun `adjusting downwards re-anchors on the exact estimate`() {
        val adjusted = product(10, rate = ConsumptionRate(3, 2)).adjust(-3, days(1))
        assertEquals(Quantity.of(11).half(), adjusted.estimatedStockAt(days(1)).exact)
        assertEquals(Quantity.of(11).half() - Quantity.of(3).half(), adjusted.estimatedStockAt(days(2)).exact)
    }

    @Test
    fun `adjusting below zero gives exactly zero`() {
        val adjusted = product(2, rate = ConsumptionRate(1, 1)).adjust(-5, t0)
        assertEquals(Quantity.ZERO, adjusted.estimatedStockAt(t0).exact)
    }

    @Test
    fun `zero adjustment is rejected`() {
        assertFailsWith<IllegalArgumentException> { product().adjust(0, t0) }
    }

    @Test
    fun `restocking two packages adds twice the package size to the exact estimate`() {
        val restocked = product(5, rate = ConsumptionRate(1, 2)).restock(2, days(1))
        assertEquals(Quantity.of(369).half(), restocked.estimatedStockAt(days(1)).exact)
    }

    @Test
    fun `restocking zero packages is rejected`() {
        assertFailsWith<IllegalArgumentException> { product().restock(0, t0) }
    }

    @Test
    fun `rate change mid-way keeps the old rate for past consumption`() {
        val changed = product(10).changeConsumptionRate(ConsumptionRate(2, 1), days(2))
        assertEquals(Quantity.of(8), changed.estimatedStockAt(days(2)).exact)
        assertEquals(Quantity.of(6), changed.estimatedStockAt(days(3)).exact)
    }

    @Test
    fun `package size change keeps stock and applies to later restocks`() {
        val resized = product(30).changePackageSize(PackageSize(60))
        assertEquals(Quantity.of(30), resized.estimatedStockAt(t0).exact)
        assertEquals(Quantity.of(90), resized.restock(1, t0).estimatedStockAt(t0).exact)
    }

    @Test
    fun `renaming and relabelling keep the stock`() {
        val edited = product(30).rename(ProductName(" Omega 3 ")).relabelUnit(UnitLabel("capsule"))
        assertEquals("Omega 3", edited.name.value)
        assertEquals("capsule", edited.unitLabel.value)
        assertEquals(Quantity.of(30), edited.estimatedStockAt(t0).exact)
    }
}
