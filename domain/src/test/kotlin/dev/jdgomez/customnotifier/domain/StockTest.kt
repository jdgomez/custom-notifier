package dev.jdgomez.customnotifier.domain

import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        assertEquals(BigInteger.valueOf(8), estimate.wholeUnits)
        assertTrue(estimate.rounded)
    }

    @Test
    fun `whole estimate is not marked rounded`() {
        val estimate = EstimatedStock(Quantity.of(8))
        assertEquals(BigInteger.valueOf(8), estimate.wholeUnits)
        assertFalse(estimate.rounded)
    }

    @Test
    fun `consumption does not overflow for large rates over long periods`() {
        val rate = ConsumptionRate(Int.MAX_VALUE, 1)
        val estimate = Stock(Quantity.of(Long.MAX_VALUE), t0).estimateAt(t0 + Duration.ofDays(100), rate)
        assertEquals(Quantity.of(Long.MAX_VALUE) - Quantity.of(Int.MAX_VALUE.toLong() * 100), estimate.exact)
    }

    @Test
    fun `extreme instants do not throw`() {
        val rate = ConsumptionRate(1, 1)
        assertEquals(Quantity.ZERO, Stock(Quantity.of(5), Instant.MIN).estimateAt(Instant.MAX, rate).exact)
        assertEquals(Quantity.of(5), Stock(Quantity.of(5), Instant.MAX).estimateAt(Instant.MIN, rate).exact)
    }

    @Test
    fun `whole units are safe above the Long range`() {
        val huge = Quantity.of(Long.MAX_VALUE) * Long.MAX_VALUE
        assertEquals(huge.floor(), EstimatedStock(huge).wholeUnits)
    }

    @Test
    fun `stock can never be negative`() {
        assertFailsWith<IllegalArgumentException> { Stock(Quantity.of(-1), t0) }
        assertFailsWith<IllegalArgumentException> { stock(1).copy(units = Quantity.of(-1)) }
    }
}
