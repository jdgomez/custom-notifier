package dev.jdgomez.customnotifier.domain

import java.math.BigInteger

/** An exact, non-negative or signed amount of units, kept as a reduced fraction of [BigInteger]s. */
class Quantity private constructor(
    private val numerator: BigInteger,
    private val denominator: BigInteger,
) : Comparable<Quantity> {
    /** True when the amount is a whole number of units. */
    val isWhole: Boolean get() = denominator == BigInteger.ONE

    /** The amount rounded down to a whole number of units. */
    fun floor(): BigInteger {
        val (quotient, remainder) = numerator.divideAndRemainder(denominator)
        return if (remainder.signum() < 0) quotient - BigInteger.ONE else quotient
    }

    operator fun plus(other: Quantity) = of(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator)

    operator fun minus(other: Quantity) = of(numerator * other.denominator - other.numerator * denominator, denominator * other.denominator)

    operator fun times(factor: Long) = of(numerator * BigInteger.valueOf(factor), denominator)

    operator fun div(divisor: Long): Quantity {
        require(divisor > 0) { "divisor must be positive" }
        return of(numerator, denominator * BigInteger.valueOf(divisor))
    }

    override fun compareTo(other: Quantity) = (numerator * other.denominator).compareTo(other.numerator * denominator)

    override fun equals(other: Any?) = other is Quantity && numerator == other.numerator && denominator == other.denominator

    override fun hashCode() = 31 * numerator.hashCode() + denominator.hashCode()

    override fun toString() = if (isWhole) "$numerator" else "$numerator/$denominator"

    companion object {
        val ZERO = of(0)

        fun of(units: Long) = Quantity(BigInteger.valueOf(units), BigInteger.ONE)

        /** The reduced fraction [numerator] / [denominator]; [denominator] must be positive. */
        fun of(
            numerator: BigInteger,
            denominator: BigInteger,
        ): Quantity {
            require(denominator.signum() > 0) { "denominator must be positive" }
            val gcd = numerator.gcd(denominator).takeIf { it.signum() > 0 } ?: BigInteger.ONE
            return Quantity(numerator / gcd, denominator / gcd)
        }
    }
}
