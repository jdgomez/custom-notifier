package dev.jdgomez.customnotifier.domain

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuantityTest {
    private fun fraction(
        n: Long,
        d: Long,
    ) = Quantity.of(BigInteger.valueOf(n), BigInteger.valueOf(d))

    @Test
    fun `fractions are reduced so equal values are equal`() {
        assertEquals(fraction(1, 2), fraction(3, 6))
        assertEquals(Quantity.of(2), fraction(10, 5))
    }

    @Test
    fun `three every two days over one day is exactly three halves`() {
        assertEquals(fraction(3, 2), Quantity.of(3) / 2)
        assertEquals(fraction(17, 2), Quantity.of(10) - Quantity.of(3) / 2)
    }

    @Test
    fun `arithmetic is exact`() {
        assertEquals(Quantity.of(1), fraction(1, 3) + fraction(2, 3))
        assertEquals(fraction(1, 6), fraction(1, 2) - fraction(1, 3))
        assertEquals(Quantity.of(2), fraction(1, 3) * 6)
    }

    @Test
    fun `floor and whole`() {
        assertEquals(BigInteger.valueOf(8), fraction(17, 2).floor())
        assertEquals(BigInteger.valueOf(-1), fraction(-1, 2).floor())
        assertTrue(Quantity.of(8).isWhole)
        assertFalse(fraction(17, 2).isWhole)
    }

    @Test
    fun `ordering`() {
        assertTrue(fraction(1, 3) < fraction(1, 2))
        assertTrue(Quantity.ZERO < fraction(1, 1000))
    }

    @Test
    fun `non-positive denominators are rejected`() {
        assertFailsWith<IllegalArgumentException> { fraction(1, 0) }
        assertFailsWith<IllegalArgumentException> { Quantity.of(1) / 0 }
    }

    @Test
    fun `numerator and denominator rebuild the same quantity`() {
        val value = fraction(34, 4)
        assertEquals(BigInteger.valueOf(17), value.numerator)
        assertEquals(BigInteger.valueOf(2), value.denominator)
        assertEquals(value, Quantity.of(value.numerator, value.denominator))
    }
}
