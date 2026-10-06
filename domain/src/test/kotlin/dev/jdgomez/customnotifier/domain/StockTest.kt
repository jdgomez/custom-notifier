package dev.jdgomez.customnotifier.domain

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StockTest {
    private val t0 = Instant.parse("2026-01-01T00:00:00Z")

    private fun stock(units: Long) = Stock(Quantity.of(units), t0)

    @Test
    fun `fractional consumption is exact`() {
        val estimate = stock(10).estimateAt(t0 + Duration.ofDays(1), ConsumptionRate(3, 2))
        assertEquals(Quantity.of(17) / 2, estimate.exact)
    }

    @Test
    fun `three every two days after one day consumes exactly one and a half`() {
        val estimate = stock(10).estimateAt(t0 + Duration.ofDays(1), ConsumptionRate(3, 2))
        assertEquals(Quantity.of(10) - Quantity.of(3) / 2, estimate.exact)
    }

    @Test
    fun `stock runs out at zero`() {
        val estimate = stock(2).estimateAt(t0 + Duration.ofDays(5), ConsumptionRate(1, 1))
        assertEquals(Quantity.ZERO, estimate.exact)
    }

    @Test
    fun `a moment before the recording means no consumption`() {
        val estimate = stock(10).estimateAt(t0 - Duration.ofDays(3), ConsumptionRate(1, 1))
        assertEquals(Quantity.of(10), estimate.exact)
    }

    @Test
    fun `fractional estimate is floored and marked rounded`() {
        val estimate = EstimatedStock(Quantity.of(17) / 2)
        assertEquals(8, estimate.wholeUnits)
        assertTrue(estimate.rounded)
    }

    @Test
    fun `whole estimate is not marked rounded`() {
        val estimate = EstimatedStock(Quantity.of(8))
        assertEquals(8, estimate.wholeUnits)
        assertFalse(estimate.rounded)
    }
}
