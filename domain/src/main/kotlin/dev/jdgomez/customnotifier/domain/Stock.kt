package dev.jdgomez.customnotifier.domain

import java.math.BigInteger
import java.time.Instant

private const val MILLIS_PER_DAY = 86_400_000L
private val NANOS_PER_SECOND = BigInteger.valueOf(1_000_000_000L)
private val NANOS_PER_MILLI = BigInteger.valueOf(1_000_000L)

/** The exact estimated stock at a moment, with its whole-unit view. */
data class EstimatedStock(
    val exact: Quantity,
) {
    /** The estimate rounded down to whole units. */
    val wholeUnits: BigInteger get() = exact.floor()

    /** True when [wholeUnits] differs from [exact] because the exact value is not whole. */
    val rounded: Boolean get() = !exact.isWhole
}

/** The stock last recorded: an exact quantity of units as of [recordedAt]. */
data class Stock(
    val units: Quantity,
    val recordedAt: Instant,
) {
    init {
        require(!units.isNegative) { "stock must not be negative" }
    }

    /** The estimate at [now] under [rate]; never below zero, and [now] before [recordedAt] counts as no time elapsed. */
    fun estimateAt(
        now: Instant,
        rate: ConsumptionRate,
    ): EstimatedStock {
        if (now <= recordedAt) return EstimatedStock(units)
        val consumed = Quantity.of(rate.units.toLong()) * elapsedMillis(now) / (rate.days * MILLIS_PER_DAY)
        return EstimatedStock((units - consumed).coerceAtLeast(Quantity.ZERO))
    }

    /** The exact moment the estimate reaches zero under [rate], floored to whole milliseconds; [recordedAt] when out of stock. */
    fun depletionMoment(rate: ConsumptionRate): Instant {
        val millis = (units * (rate.days * MILLIS_PER_DAY) / rate.units.toLong()).floor()
        return recordedAt.plusMillis(millis.longValueExact())
    }

    /** Whole milliseconds from [recordedAt] to [now], exact for any pair of instants. */
    private fun elapsedMillis(now: Instant): BigInteger {
        val seconds = BigInteger.valueOf(now.epochSecond) - BigInteger.valueOf(recordedAt.epochSecond)
        val nanos = seconds * NANOS_PER_SECOND + BigInteger.valueOf((now.nano - recordedAt.nano).toLong())
        return nanos / NANOS_PER_MILLI
    }
}
